package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(nullable = false)
    private Role role;

    @Column(name = "birth_year")
    private Integer birthYear;

    // TODO: PG enum 매핑 수정 필요 — users.gender 컬럼은 PostgreSQL gender_type enum 인데
    //   @Enumerated(EnumType.STRING) 은 VARCHAR 로 BIND → INSERT/UPDATE 시 500 에러.
    //   @JdbcTypeCode(SqlTypes.NAMED_ENUM) + columnDefinition="gender_type" 으로 교체 필요.
    //   (docs/NEXT_STEPS.md §1.1 참조 — register 계열 통합 테스트 전부 이 이슈로 @Disabled)
    @Enumerated(EnumType.STRING)
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
}
