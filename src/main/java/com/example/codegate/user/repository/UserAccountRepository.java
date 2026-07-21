package com.example.codegate.user.repository;

import com.example.codegate.user.entity.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByKakaoId(String kakaoId);

    Optional<UserAccount> findByLoginId(String loginId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :userId")
    Optional<UserAccount> findByIdForUpdate(@Param("userId") Long userId);

    boolean existsByLoginId(String loginId);
}
