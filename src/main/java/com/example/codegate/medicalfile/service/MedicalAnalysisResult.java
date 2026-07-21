package com.example.codegate.medicalfile.service;

public record MedicalAnalysisResult(
        String summary,
        String recommendedFood,
        String recommendedExercise,
        Integer bloodPressureScorePercent,
        Integer bloodSugarScorePercent,
        Integer gammaGtpScorePercent
) {
}
