package com.example.codegate.user.repository;

import com.example.codegate.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByKakaoId(String kakaoId);

    Optional<UserAccount> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
