package com.example.codegate.medicalfile.service;

public record StoredMedicalFile(
        String originalFileName,
        String storedFileName,
        String contentType,
        long fileSize,
        String storagePath
) {
}
