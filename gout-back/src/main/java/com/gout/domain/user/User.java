package com.gout.domain.user;

import com.gout.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
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
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "kakao_id")
    private String kakaoId;

    @Column(name = "consent_sensitive_at")
    private java.time.LocalDateTime consentSensitiveAt;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void consentToSensitiveData() {
        this.consentSensitiveAt = java.time.LocalDateTime.now();
    }

    public enum Role { USER, ADMIN }
    public enum Gender { MALE, FEMALE, OTHER }
}
