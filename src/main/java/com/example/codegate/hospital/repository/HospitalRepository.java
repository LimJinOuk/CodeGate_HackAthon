package com.example.codegate.hospital.repository;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Optional<Hospital> findByUserAccount(UserAccount userAccount);
}
