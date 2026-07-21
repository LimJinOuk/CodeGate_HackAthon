package com.example.codegate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codegate.upload.medical-file")
public record MedicalFileUploadProperties(
        String rootDir,
        long maxFileSizeBytes
) {
}
