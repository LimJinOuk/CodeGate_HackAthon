package com.example.codegate.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_kakao_id", columnNames = "kakao_id"),
                @UniqueConstraint(name = "uk_user_login_id", columnNames = "login_id")
        }
)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoginType loginType;

    @Column(name = "kakao_id", length = 80)
    private String kakaoId;

    @Column(name = "login_id", length = 80)
    private String loginId;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected UserAccount() {
    }

    private UserAccount(UserRole role, LoginType loginType, String kakaoId, String loginId, String passwordHash) {
        this.role = role;
        this.loginType = loginType;
        this.kakaoId = kakaoId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static UserAccount kakaoPatient(String kakaoId) {
        return new UserAccount(UserRole.PATIENT, LoginType.KAKAO, kakaoId, null, null);
    }

    public static UserAccount hospitalLocal(String loginId, String passwordHash) {
        return new UserAccount(UserRole.HOSPITAL, LoginType.HOSPITAL_LOCAL, null, loginId, passwordHash);
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public LoginType getLoginType() {
        return loginType;
    }

    public String getKakaoId() {
        return kakaoId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
