package com.example.codegate.medicalfile.entity;

import com.example.codegate.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_files")
public class MedicalFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private UserAccount patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MedicalFileType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MedicalFileStatus status;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String storedFileName;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 1000)
    private String storagePath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected MedicalFile() {
    }

    public MedicalFile(UserAccount patient,
                       MedicalFileType type,
                       String originalFileName,
                       String storedFileName,
                       String contentType,
                       long fileSize,
                       String storagePath,
                       LocalDateTime createdAt) {
        this.patient = patient;
        this.type = type;
        this.status = MedicalFileStatus.UPLOADED;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getPatient() {
        return patient;
    }

    public Long getPatientId() {
        return patient.getId();
    }

    public MedicalFileType getType() {
        return type;
    }

    public MedicalFileStatus getStatus() {
        return status;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markOcrPending() {
        this.status = MedicalFileStatus.OCR_PENDING;
    }

    public void markOcrProcessing() {
        this.status = MedicalFileStatus.OCR_PROCESSING;
    }

    public void markOcrCompleted() {
        this.status = MedicalFileStatus.OCR_COMPLETED;
    }

    public void markOcrFailed() {
        this.status = MedicalFileStatus.OCR_FAILED;
    }
}
