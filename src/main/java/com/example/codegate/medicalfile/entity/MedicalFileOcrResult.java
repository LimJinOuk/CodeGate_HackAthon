package com.example.codegate.medicalfile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_file_ocr_results")
public class MedicalFileOcrResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_file_id", nullable = false, unique = true)
    private MedicalFile medicalFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MedicalFileOcrStatus status;

    @Lob
    private String extractedText;

    @Lob
    private String summary;

    @Lob
    private String recommendedFood;

    @Lob
    private String recommendedExercise;

    private Integer bloodPressureScorePercent;

    private Integer bloodSugarScorePercent;

    private Integer gammaGtpScorePercent;

    @Column(length = 80)
    private String modelName;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected MedicalFileOcrResult() {
    }

    public MedicalFileOcrResult(MedicalFile medicalFile, LocalDateTime now) {
        this.medicalFile = medicalFile;
        this.status = MedicalFileOcrStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public MedicalFile getMedicalFile() {
        return medicalFile;
    }

    public MedicalFileOcrStatus getStatus() {
        return status;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public String getSummary() {
        return summary;
    }

    public String getRecommendedFood() {
        return recommendedFood;
    }

    public String getRecommendedExercise() {
        return recommendedExercise;
    }

    public Integer getBloodPressureScorePercent() {
        return bloodPressureScorePercent;
    }

    public Integer getBloodSugarScorePercent() {
        return bloodSugarScorePercent;
    }

    public Integer getGammaGtpScorePercent() {
        return gammaGtpScorePercent;
    }

    public String getModelName() {
        return modelName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing(String modelName, LocalDateTime now) {
        this.status = MedicalFileOcrStatus.PROCESSING;
        this.modelName = modelName;
        this.errorMessage = null;
        this.updatedAt = now;
    }

    public void complete(String extractedText,
                         String summary,
                         String recommendedFood,
                         String recommendedExercise,
                         Integer bloodPressureScorePercent,
                         Integer bloodSugarScorePercent,
                         Integer gammaGtpScorePercent,
                         LocalDateTime now) {
        this.status = MedicalFileOcrStatus.COMPLETED;
        this.extractedText = extractedText;
        this.summary = summary;
        this.recommendedFood = recommendedFood;
        this.recommendedExercise = recommendedExercise;
        this.bloodPressureScorePercent = bloodPressureScorePercent;
        this.bloodSugarScorePercent = bloodSugarScorePercent;
        this.gammaGtpScorePercent = gammaGtpScorePercent;
        this.errorMessage = null;
        this.updatedAt = now;
    }

    public void fail(String errorMessage, LocalDateTime now) {
        this.status = MedicalFileOcrStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = now;
    }
}
