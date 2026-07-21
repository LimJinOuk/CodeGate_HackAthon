package com.example.codegate.medicalfile.service;

import com.example.codegate.medicalfile.dto.MedicalFileOcrResultResponse;
import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileOcrResult;
import com.example.codegate.medicalfile.repository.MedicalFileOcrResultRepository;
import com.example.codegate.medicalfile.repository.MedicalFileRepository;
import com.example.codegate.medicalfile.support.MedicalFileErrors;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalFileOcrResultService {

    private final MedicalFileRepository medicalFileRepository;
    private final MedicalFileOcrResultRepository ocrResultRepository;

    public MedicalFileOcrResultService(MedicalFileRepository medicalFileRepository,
                                       MedicalFileOcrResultRepository ocrResultRepository) {
        this.medicalFileRepository = medicalFileRepository;
        this.ocrResultRepository = ocrResultRepository;
    }

    @Transactional(readOnly = true)
    public MedicalFileOcrResultResponse findMine(UserAccount patient, Long medicalFileId) {
        MedicalFile medicalFile = medicalFileRepository.findByIdAndPatient(medicalFileId, patient)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        MedicalFileOcrResult result = ocrResultRepository.findByMedicalFile(medicalFile)
                .orElseThrow(MedicalFileErrors::ocrResultNotFound);
        return MedicalFileOcrResultResponse.from(result);
    }
}
