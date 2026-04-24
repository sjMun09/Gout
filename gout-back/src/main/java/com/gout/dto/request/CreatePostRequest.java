package com.gout.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreatePostRequest {

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
     */
    private List<String> imageUrls;
}
