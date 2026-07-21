package com.example.codegate.medicalfile.dto;

import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileStatus;
import com.example.codegate.medicalfile.entity.MedicalFileType;

import java.time.LocalDateTime;

public record MedicalFileResponse(
        Long id,
        MedicalFileType type,
        MedicalFileStatus status,
        String originalFileName,
        String contentType,
        long fileSize,
        LocalDateTime createdAt
) {

    public static MedicalFileResponse from(MedicalFile medicalFile) {
        return new MedicalFileResponse(
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
