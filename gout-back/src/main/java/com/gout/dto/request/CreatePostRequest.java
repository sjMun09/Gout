package com.gout.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.regex.Pattern;

@Getter
@Setter
public class CreatePostRequest {

    // /api/uploads/posts 업로드 응답으로만 얻을 수 있는 형식: 32자 hex 파일명 + 허용 확장자.
    // UploadController 가 UUID.replace("-","") 로 생성하고 png/jpg/webp 만 허용하므로 완전 동기화된 화이트리스트.
    private static final Pattern IMAGE_URL_PATTERN =
            Pattern.compile("^/api/uploads/posts/[a-f0-9]{32}\\.(png|jpg|webp)$");
    private static final int MAX_IMAGE_URLS = 5;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 500, message = "제목은 500자 이내여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private String category = "FREE";

    private boolean isAnonymous = false;

    /**
     * 첨부 이미지 URL 목록 (선택).
     * 클라이언트는 먼저 /api/uploads/posts 로 업로드해 받은 상대 URL을 여기에 담아 보낸다.
     * 외부 URL 직접 주입 방지 — {@link #IMAGE_URL_PATTERN} 만 허용.
     */
    private List<String> imageUrls;

    /**
     * 해시태그 목록 (선택). 최대 10개, 각 태그 1~50자, 허용 문자: 한글·영문·숫자·_
     */
    private List<String> tags;

    @AssertTrue(message = "태그는 최대 10개까지 입력할 수 있습니다.")
    private boolean isTagCountValid() {
        return tags == null || tags.size() <= 10;
    }

    @AssertTrue(message = "태그는 한글·영문·숫자·_ 조합, 1~50자여야 합니다.")
    private boolean isTagFormatValid() {
        if (tags == null) return true;
        return tags.stream().allMatch(t ->
                t != null && t.matches("^[가-힣A-Za-z0-9_]{1,50}$")
        );
    }

    @AssertTrue(message = "이미지는 최대 5장까지 첨부할 수 있습니다.")
    private boolean isImageUrlCountValid() {
        return imageUrls == null || imageUrls.size() <= MAX_IMAGE_URLS;
    }

    @AssertTrue(message = "업로드 API 로 발급받은 이미지 경로만 사용할 수 있습니다.")
    private boolean isImageUrlFormatValid() {
        if (imageUrls == null) return true;
        return imageUrls.stream().allMatch(u ->
                u != null && IMAGE_URL_PATTERN.matcher(u).matches()
        );
    }
}
