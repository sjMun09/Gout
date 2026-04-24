package com.gout.controller;

import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final Path uploadDir;

    public UploadController(@Value("${app.uploads.base-dir:/app/uploads}") String baseDir) {
        this.uploadDir = Paths.get(baseDir, "posts").toAbsolutePath().normalize();
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
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "허용되지 않은 이미지 형식입니다: " + contentType);
            }

            String ext = MIME_TO_EXT.get(contentType.toLowerCase(Locale.ROOT));
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = uploadDir.resolve(filename).normalize();
            if (!target.startsWith(uploadDir)) {
                // 이론상 UUID 파일명이므로 발생할 수 없으나 방어.
                throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 파일 경로입니다.");
            }

            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
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

        MediaType contentType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        Resource resource = new FileSystemResource(target);
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentLength(Files.size(target))
                .body(resource);
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
