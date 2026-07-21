package com.example.codegate.medicalfile.service;

import com.example.codegate.config.OcrProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class MedicalFileOcrProcessingService {

    private final MedicalFileOcrTransactionService transactionService;
    private final LocalMedicalFileStorageService storageService;
    private final MedicalFileOcrPreprocessor preprocessor;
    private final AnthropicClaudeClient claudeClient;
    private final OcrProperties ocrProperties;

    public MedicalFileOcrProcessingService(MedicalFileOcrTransactionService transactionService,
                                           LocalMedicalFileStorageService storageService,
                                           MedicalFileOcrPreprocessor preprocessor,
                                           AnthropicClaudeClient claudeClient,
                                           OcrProperties ocrProperties) {
        this.transactionService = transactionService;
        this.storageService = storageService;
        this.preprocessor = preprocessor;
        this.claudeClient = claudeClient;
        this.ocrProperties = ocrProperties;
    }

    @Async("ocrTaskExecutor")
    public void processAsync(Long medicalFileId) {
        try {
            OcrProcessingTarget target = transactionService.markProcessing(medicalFileId, claudeClient.modelName());
            Path filePath = storageService.resolveForRead(target.storagePath());
            List<OcrImagePayload> images = preprocessor.prepareImages(filePath, target.originalFileName());

            String extractedText = extractAllText(images);
            MedicalAnalysisResult analysisResult = claudeClient.analyze(extractedText, target.patientAge());

            transactionService.complete(medicalFileId, extractedText, analysisResult);
        } catch (Exception exception) {
            transactionService.fail(medicalFileId, rootMessage(exception));
        }
    }

    private String extractAllText(List<OcrImagePayload> images) {
        int pagesPerRequest = Math.max(1, ocrProperties.pagesPerRequest());
        List<String> pageTexts = new ArrayList<>();

        for (int startIndex = 0; startIndex < images.size(); startIndex += pagesPerRequest) {
            int endIndex = Math.min(startIndex + pagesPerRequest, images.size());
            List<OcrImagePayload> batch = images.subList(startIndex, endIndex);
            String batchText = claudeClient.extractText(batch, startIndex + 1);
            pageTexts.add(batchText);
        }

        return String.join("\n\n", pageTexts);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
