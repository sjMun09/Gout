package com.gout.controller;

import com.gout.config.properties.UploadsProperties;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 게시글 이미지 업로드 / 제공 컨트롤러.
 *
 * <p>저장 전략: 로컬 파일시스템 ({@code app.uploads.base-dir}/posts/{uuid}.{ext}).
 * S3/MinIO 없이 먼저 동작하는 단순 구현. 컨테이너에서는
 * /app/uploads 를 볼륨 마운트 하여 재기동에도 파일이 남도록 한다.</p>
 *
 * <p>업로드(POST)는 인증 필요. 조회(GET)는 공개
 * (posts GET 이 공개되어 있으니 일관성 유지).</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/uploads/posts")
public class UploadController {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final int MAX_FILES = 5;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );
    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp"
    );

    // 서빙 시 strict MediaType 매핑. MediaTypeFactory(filename) 는 확장자 의존 + 알 수 없는
    // 확장자에 octet-stream 폴백을 하므로, 매직바이트 결과로 결정된 확장자만 명시 매핑한다 (MED-003).
    private static final Map<String, MediaType> EXT_TO_MEDIATYPE = Map.of(
            "png", MediaType.IMAGE_PNG,
            "jpg", MediaType.IMAGE_JPEG,
            "webp", MediaType.parseMediaType("image/webp")
    );

    private final Path uploadDir;

    public UploadController(UploadsProperties uploadsProperties) {
        this.uploadDir = Paths.get(uploadsProperties.baseDir(), "posts").toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(uploadDir);
        log.info("Post image upload directory: {}", uploadDir);
    }

    /**
     * 이미지 업로드 (multipart/form-data).
     * 필드명: {@code files}, 최대 {@value #MAX_FILES}개, 각 {@value #MAX_FILE_SIZE_BYTES} 바이트 이하.
     * 허용 MIME: image/png, image/jpeg, image/webp.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> upload(
            @RequestParam("files") MultipartFile[] files) throws IOException {
        requireAuthenticated();

        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "업로드할 파일이 없습니다.");
        }
        if (files.length > MAX_FILES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "이미지는 최대 " + MAX_FILES + "장까지 업로드할 수 있습니다.");
        }

        List<String> urls = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "빈 파일이 포함되어 있습니다.");
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "파일 크기가 5MB 를 초과했습니다: " + file.getOriginalFilename());
            }
            String headerMime = file.getContentType();
            if (headerMime == null || !ALLOWED_MIME_TYPES.contains(headerMime.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "허용되지 않은 이미지 형식입니다: " + headerMime);
            }

            // MED-003: Content-Type 헤더만 신뢰하지 않고 매직바이트로 실제 포맷 검증.
            // 5MB 상한이 이미 걸려 있어 getBytes() 메모리 로드 안전.
            byte[] bytes = file.getBytes();
            String detectedMime = detectImageMime(bytes);
            if (detectedMime == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "이미지 파일 매직바이트 검증 실패: " + file.getOriginalFilename());
            }
            if (!detectedMime.equals(headerMime.toLowerCase(Locale.ROOT))) {
                // 헤더 위조 — 클라이언트가 image/png 라고 주장했지만 실제 바이트는 다른 포맷.
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "Content-Type 과 실제 파일 형식이 일치하지 않습니다.");
            }

            String ext = MIME_TO_EXT.get(detectedMime);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = uploadDir.resolve(filename).normalize();
            if (!target.startsWith(uploadDir)) {
                // 이론상 UUID 파일명이므로 발생할 수 없으나 방어.
                throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 파일 경로입니다.");
            }

            Files.write(target, bytes);
            urls.add("/api/uploads/posts/" + filename);
        }

        return ResponseEntity.ok(ApiResponse.success("업로드 완료", Map.of("urls", urls)));
    }

    /**
     * 업로드된 이미지 스트리밍. {@code ..} 같은 traversal 방지 검증 포함.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) throws IOException {
        if (filename == null || filename.isBlank()
                || filename.contains("..")
                || filename.contains("/")
                || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir) || !Files.exists(target) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        // MED-003: 확장자 → strict MediaType. MediaTypeFactory 는 알 수 없는 확장자에
        // octet-stream 폴백을 하지만, 업로드 시 매직바이트 통과한 png/jpg/webp 만 저장되므로
        // 매핑에 없는 확장자는 비정상 상태 → 404 로 응답.
        int dot = filename.lastIndexOf('.');
        String ext = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        MediaType contentType = EXT_TO_MEDIATYPE.get(ext);
        if (contentType == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(target);
        return ResponseEntity.ok()
                .contentType(contentType)
                // MED-003: SecurityFilterChain 의 nosniff 헤더가 캐시된 응답에 누락될 가능성을
                // 응답에 직접 박아 차단. 브라우저가 MIME sniffing 으로 image 응답을 HTML 로 해석 못 하게.
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentLength(Files.size(target))
                .body(resource);
    }

    /**
     * 이미지 매직바이트로 MIME 추정. 알려진 PNG/JPEG/WEBP 시그니처만 인정.
     * 결과는 {@link #ALLOWED_MIME_TYPES} 와 동일한 lowercase 문자열, 매칭 없으면 null.
     */
    private static String detectImageMime(byte[] bytes) {
        if (bytes == null) return null;
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        // JPEG: FF D8 FF (SOI 마커)
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // WEBP: RIFF....WEBP (4바이트 길이 사이에 두고 8~11에 WEBP)
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private void requireAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            return;
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return;
        }
        throw new AccessDeniedException("로그인이 필요합니다.");
    }
}
