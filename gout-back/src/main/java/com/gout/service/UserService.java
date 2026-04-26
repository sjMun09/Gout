package com.gout.service;

import com.gout.dto.request.ChangePasswordRequest;
import com.gout.dto.request.EditProfileRequest;
import com.gout.dto.response.UserProfileResponse;

public interface UserService {

    /** GET /api/me */
    UserProfileResponse getMe(String userId);

    /** PATCH /api/me — 닉네임/생년/성별 부분 수정. */
    UserProfileResponse editProfile(String userId, EditProfileRequest request);

    /** POST /api/me/password — 현재 비밀번호 검증 후 교체. */
    void changePassword(String userId, ChangePasswordRequest request);

    /** POST /api/me/sensitive-consent — 민감 건강정보 수집·이용 동의. */
    UserProfileResponse consentSensitiveData(String userId);

    /** DELETE /api/me/sensitive-consent — 민감 건강정보 수집·이용 동의 철회. */
    UserProfileResponse withdrawSensitiveDataConsent(String userId);

    /** DELETE /api/me — Soft Delete. */
    void withdraw(String userId);
}
