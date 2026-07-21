package com.example.codegate.user.repository;

import com.example.codegate.user.entity.PatientProfile;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {

    Optional<PatientProfile> findByUserAccount(UserAccount userAccount);
}
