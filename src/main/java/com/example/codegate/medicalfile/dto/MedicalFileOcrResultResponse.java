package com.example.codegate.medicalfile.dto;

import com.example.codegate.medicalfile.entity.MedicalFileOcrResult;
import com.example.codegate.medicalfile.entity.MedicalFileOcrStatus;

import java.time.LocalDateTime;

public record MedicalFileOcrResultResponse(
        Long medicalFileId,
        MedicalFileOcrStatus status,
        String extractedText,
        String summary,
        String recommendedFood,
        String recommendedExercise,
        Integer bloodPressureScorePercent,
        Integer bloodSugarScorePercent,
        Integer gammaGtpScorePercent,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MedicalFileOcrResultResponse from(MedicalFileOcrResult result) {
        return new MedicalFileOcrResultResponse(
                result.getMedicalFile().getId(),
                result.getStatus(),
                result.getExtractedText(),
                result.getSummary(),
                result.getRecommendedFood(),
                result.getRecommendedExercise(),
                result.getBloodPressureScorePercent(),
                result.getBloodSugarScorePercent(),
                result.getGammaGtpScorePercent(),
                result.getErrorMessage(),
                result.getCreatedAt(),
                result.getUpdatedAt()
        );
    }
}
