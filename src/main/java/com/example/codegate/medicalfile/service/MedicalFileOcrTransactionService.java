package com.example.codegate.medicalfile.service;

import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileOcrResult;
import com.example.codegate.medicalfile.repository.MedicalFileOcrResultRepository;
import com.example.codegate.medicalfile.repository.MedicalFileRepository;
import com.example.codegate.medicalfile.support.MedicalFileErrors;
import com.example.codegate.user.entity.PatientProfile;
import com.example.codegate.user.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Service
public class MedicalFileOcrTransactionService {

    private final MedicalFileRepository medicalFileRepository;
    private final MedicalFileOcrResultRepository ocrResultRepository;
    private final PatientProfileRepository patientProfileRepository;

    public MedicalFileOcrTransactionService(MedicalFileRepository medicalFileRepository,
                                            MedicalFileOcrResultRepository ocrResultRepository,
                                            PatientProfileRepository patientProfileRepository) {
        this.medicalFileRepository = medicalFileRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    @Transactional
    public MedicalFileOcrResult createPending(MedicalFile medicalFile) {
        medicalFile.markOcrPending();
        return ocrResultRepository.save(new MedicalFileOcrResult(medicalFile, LocalDateTime.now()));
    }

    @Transactional
    public OcrProcessingTarget markProcessing(Long medicalFileId, String modelName) {
        MedicalFile medicalFile = medicalFileRepository.findById(medicalFileId)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        MedicalFileOcrResult result = ocrResultRepository.findByMedicalFile(medicalFile)
                .orElseThrow(MedicalFileErrors::ocrResultNotFound);

        LocalDateTime now = LocalDateTime.now();
        medicalFile.markOcrProcessing();
        result.markProcessing(modelName, now);

        Integer age = patientProfileRepository.findByUserAccount(medicalFile.getPatient())
                .map(PatientProfile::getBirthDate)
                .map(this::ageOf)
                .orElse(null);

        return new OcrProcessingTarget(
                medicalFile.getId(),
                medicalFile.getOriginalFileName(),
                medicalFile.getStoragePath(),
                age
        );
    }

    @Transactional
    public void complete(Long medicalFileId, String extractedText, MedicalAnalysisResult analysisResult) {
        MedicalFile medicalFile = medicalFileRepository.findById(medicalFileId)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        MedicalFileOcrResult result = ocrResultRepository.findByMedicalFile(medicalFile)
                .orElseThrow(MedicalFileErrors::ocrResultNotFound);

        medicalFile.markOcrCompleted();
        result.complete(
                extractedText,
                analysisResult.summary(),
                analysisResult.recommendedFood(),
                analysisResult.recommendedExercise(),
                analysisResult.bloodPressureScorePercent(),
                analysisResult.bloodSugarScorePercent(),
                analysisResult.gammaGtpScorePercent(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public void fail(Long medicalFileId, String errorMessage) {
        MedicalFile medicalFile = medicalFileRepository.findById(medicalFileId)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        MedicalFileOcrResult result = ocrResultRepository.findByMedicalFile(medicalFile)
                .orElseThrow(MedicalFileErrors::ocrResultNotFound);

        medicalFile.markOcrFailed();
        result.fail(trimErrorMessage(errorMessage), LocalDateTime.now());
    }

    private Integer ageOf(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String trimErrorMessage(String errorMessage) {
        String message = errorMessage == null || errorMessage.isBlank()
                ? "OCR 처리 중 오류가 발생했습니다."
                : errorMessage;
        if (message.length() > 1000) {
            return message.substring(0, 1000);
        }
        return message;
    }
}
