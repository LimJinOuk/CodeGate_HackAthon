package com.example.codegate.medicalfile.service;

public record OcrImagePayload(
        String label,
        String mediaType,
        String base64Data
) {
}
