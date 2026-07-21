package com.example.codegate.medicalfile.repository;

import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileOcrResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalFileOcrResultRepository extends JpaRepository<MedicalFileOcrResult, Long> {

    Optional<MedicalFileOcrResult> findByMedicalFile(MedicalFile medicalFile);
}
