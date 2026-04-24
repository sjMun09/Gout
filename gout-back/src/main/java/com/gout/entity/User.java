package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_role")
    private Role role;

    @Column(name = "birth_year")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "gender_type")
    private Gender gender;

    @Column(name = "kakao_id")
    private String kakaoId;

    @Column(name = "consent_sensitive_at")
    private LocalDateTime consentSensitiveAt;

    // 빌더로 설정 가능한 필드를 명시적으로 통제
    // id, role, kakaoId, consentSensitiveAt은 외부에서 직접 설정 불가
    @Builder
    private User(String email, String password, String nickname,
                 Integer birthYear, Gender gender) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.birthYear = birthYear;
        this.gender = gender;
        this.role = Role.USER;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void consentToSensitiveData() {
        this.consentSensitiveAt = LocalDateTime.now();
    }

    public void linkKakao(String kakaoId) {
        this.kakaoId = kakaoId;
    }

    public enum Role { USER, ADMIN }
    public enum Gender { MALE, FEMALE, OTHER }

    // ==================== Agent-H: 프로필 수정 / 비밀번호 변경 / 탈퇴 ====================
    // 필드/메서드 모두 클래스 말미에 위치 — Agent-A 의 gender 컬럼 매핑 수정과 충돌하지 않도록 분리.

    public enum Status { ACTIVE, SUSPENDED, DELETED }

    // V23__add_user_status.sql 로 추가한 컬럼. DEFAULT 'ACTIVE' 이지만 JPA 반영을 위해 @PrePersist 에서 설정.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @PrePersist
    private void prePersistStatus() {
        if (this.status == null) {
            this.status = Status.ACTIVE;
        }
    }

    /**
     * 프로필 부분 수정. null 인 필드는 변경하지 않는다.
     */
    public void editProfile(String nickname, Integer birthYear, Gender gender) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (birthYear != null) {
            this.birthYear = birthYear;
        }
        if (gender != null) {
            this.gender = gender;
        }
    }

    /**
     * 비밀번호 변경. 호출측에서 이미 해시된 값을 전달해야 한다(BCrypt).
     */
    public void changePassword(String newHashedPassword) {
        this.password = newHashedPassword;
    }

    /**
     * 회원 탈퇴(Soft Delete). status 를 DELETED 로만 바꾼다.
     * CustomUserDetailsService 가 DELETED 사용자를 비활성화하므로 이후 토큰으로 접근 불가.
     */
    public void withdraw() {
        this.status = Status.DELETED;
    }
}
