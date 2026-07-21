package com.example.codegate.medicalfile.dto;

import org.springframework.core.io.Resource;

public record MedicalFileContentResponse(
        String originalFileName,
        String contentType,
        long fileSize,
        Resource resource
) {
}
