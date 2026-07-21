package com.example.codegate.medicalfile.repository;

import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalFileRepository extends JpaRepository<MedicalFile, Long> {

    List<MedicalFile> findByPatientOrderByCreatedAtDesc(UserAccount patient);

    Optional<MedicalFile> findByIdAndPatient(Long id, UserAccount patient);

    List<MedicalFile> findByPatient_IdOrderByCreatedAtDesc(Long patientId);

    Optional<MedicalFile> findByIdAndPatient_Id(Long id, Long patientId);
}
