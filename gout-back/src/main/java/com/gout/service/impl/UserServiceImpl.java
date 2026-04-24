package com.gout.service.impl;

import com.gout.dao.UserRepository;
import com.gout.dto.request.ChangePasswordRequest;
import com.gout.dto.request.EditProfileRequest;
import com.gout.dto.response.UserProfileResponse;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMe(String userId) {
        User user = findActive(userId);
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public UserProfileResponse editProfile(String userId, EditProfileRequest request) {
        User user = findActive(userId);
        user.editProfile(request.getNickname(), request.getBirthYear(), request.getGender());
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findActive(userId);

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    @Transactional
    public void withdraw(String userId) {
        User user = findActive(userId);
        user.withdraw();
    }

    private User findActive(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == User.Status.DELETED) {
            // 탈퇴된 사용자는 일반 사용자에게 404 처럼 보여준다.
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
