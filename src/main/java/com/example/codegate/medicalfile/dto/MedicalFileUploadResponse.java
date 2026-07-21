package com.example.codegate.medicalfile.dto;

import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileStatus;
import com.example.codegate.medicalfile.entity.MedicalFileType;

import java.time.LocalDateTime;

public record MedicalFileUploadResponse(
        Long id,
        MedicalFileType type,
        MedicalFileStatus status,
        String originalFileName,
        String contentType,
        long fileSize,
        LocalDateTime createdAt
) {

    public static MedicalFileUploadResponse from(MedicalFile medicalFile) {
        return new MedicalFileUploadResponse(
                medicalFile.getId(),
                medicalFile.getType(),
                medicalFile.getStatus(),
                medicalFile.getOriginalFileName(),
                medicalFile.getContentType(),
                medicalFile.getFileSize(),
                medicalFile.getCreatedAt()
        );
    }
}
