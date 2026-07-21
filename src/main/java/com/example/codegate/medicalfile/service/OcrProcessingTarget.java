package com.example.codegate.medicalfile.service;

public record OcrProcessingTarget(
        Long medicalFileId,
        String originalFileName,
        String storagePath,
        Integer patientAge
) {
}
