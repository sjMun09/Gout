package com.gout.dto.request;

import com.gout.entity.User;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PATCH /api/me — 부분 수정. 모든 필드가 null 일 수 있다.
 */
@Getter
@Setter
@NoArgsConstructor
public class EditProfileRequest {

    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
    private String nickname;

    private Integer birthYear;

    private User.Gender gender;
}
