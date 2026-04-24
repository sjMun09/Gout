package com.gout.service.impl;

import com.gout.dao.GoutAttackLogRepository;
import com.gout.dao.HospitalReviewRepository;
import com.gout.dao.MedicationLogRepository;
import com.gout.dao.NotificationRepository;
import com.gout.dao.PostBookmarkRepository;
import com.gout.dao.PostLikeRepository;
import com.gout.dao.ReportRepository;
import com.gout.dao.UricAcidLogRepository;
import com.gout.dao.UserRepository;
import com.gout.dto.request.ChangePasswordRequest;
import com.gout.dto.request.EditProfileRequest;
import com.gout.dto.response.UserProfileResponse;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.AuthService;
import com.gout.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    // === 탈퇴 시 앱 레이어 cascade 대상 Repository ===
    // V24 에서 DB 레벨 FK 를 모두 제거했기 때문에, 참조 무결성은 여기서 명시적으로 보장한다.
    private final UricAcidLogRepository uricAcidLogRepository;
    private final GoutAttackLogRepository goutAttackLogRepository;
    private final MedicationLogRepository medicationLogRepository;
    private final HospitalReviewRepository hospitalReviewRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;

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
        // P1-10: 비밀번호 변경 시 기존 리프레시 세션을 모두 폐기 — 다른 기기/브라우저 강제 로그아웃.
        authService.forceLogout(userId);
    }

    /**
     * 회원 탈퇴.
     *
     * 팀장 결정 사항 (docs/26-04-24_DB-PK-FK-재설계.md 팀장 결정 사항):
     *   - post_likes / post_bookmarks → 물리 삭제 (Right to be forgotten)
     *   - hospital_reviews.user_id    → NULL 치환 (리뷰 콘텐츠는 유지, 작성자만 익명화)
     *   - reports.reporter_id         → NULL 치환 (감사 목적으로 신고 기록은 유지)
     *   - uric_acid_logs / gout_attack_logs / medication_logs → 물리 삭제 (GDPR, 민감정보)
     *   - notifications (user_id = 탈퇴자) → 물리 삭제 (수신자가 접근 불가)
     *   - posts / comments            → user_id 유지 (삭제하지 않음)
     *                                    응답 DTO 에서 "탈퇴한 사용자" 로 표시 (UserNicknameResolver 참조)
     *   - users                        → status = DELETED (soft delete 유지, 감사 로그 목적)
     *
     * 모든 처리는 단일 @Transactional 안에서 순서대로 실행. 실패 시 전체 롤백.
     * V24 로 DB FK 가 제거되었으므로 여기서 반드시 정리하지 않으면 고아 row 가 남는다.
     */
    @Override
    @Transactional
    public void withdraw(String userId) {
        User user = findActive(userId);

        // 1) 개인 건강 기록 3종 — 물리 삭제 (GDPR)
        int uricAcid   = uricAcidLogRepository.deleteByUserId(userId);
        int goutAttack = goutAttackLogRepository.deleteByUserId(userId);
        int medication = medicationLogRepository.deleteByUserId(userId);

        // 2) 커뮤니티 상호작용 — 물리 삭제 (Right to be forgotten)
        int likes     = postLikeRepository.deleteByUserId(userId);
        int bookmarks = postBookmarkRepository.deleteByUserId(userId);

        // 3) 알림 — 수신자(user_id) 기준으로만 삭제. from_user_id 컬럼이 없으므로 단일 쿼리.
        int notifications = notificationRepository.deleteByUserId(userId);

        // 4) 병원 리뷰 — 작성자 익명화 (리뷰 본문 유지)
        int reviewsAnon = hospitalReviewRepository.anonymizeByUserId(userId);

        // 5) 신고 — 신고자 익명화 (감사 로그 유지)
        int reportsAnon = reportRepository.anonymizeByReporterId(userId);

        // 6) 마지막으로 users.status = DELETED (soft delete 유지)
        //    posts / comments 의 user_id 는 변경하지 않는다 — 응답 렌더링 시점에
        //    UserNicknameResolver 가 status=DELETED 를 감지해 "탈퇴한 사용자" 로 표시한다.
        user.withdraw();

        // P1-10: 탈퇴와 동시에 남아있던 refresh 세션을 폐기. access 는 최대 15분 내 자연 만료.
        // DB 커밋 실패 시에도 Redis 무효화가 먼저 일어나면 안 되므로 withdraw() 뒤에 둔다.
        authService.forceLogout(userId);

        log.info("User withdrawn userId={} cascades=[uricAcid:{}, goutAttack:{}, medication:{}, "
                        + "likes:{}, bookmarks:{}, notifications:{}, reviewsAnonymized:{}, reportsAnonymized:{}]",
                userId, uricAcid, goutAttack, medication,
                likes, bookmarks, notifications, reviewsAnon, reportsAnon);
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
