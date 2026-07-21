package com.example.codegate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codegate.ocr")
public record OcrProperties(
        int maxPdfPages,
        int pdfRenderDpi,
        int maxImageLongEdge,
        float jpegQuality,
        int pagesPerRequest
) {
}
