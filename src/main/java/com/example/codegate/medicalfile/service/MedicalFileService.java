package com.example.codegate.medicalfile.service;

import com.example.codegate.medicalfile.dto.MedicalFileUploadResponse;
import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileType;
import com.example.codegate.medicalfile.repository.MedicalFileRepository;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class MedicalFileService {

    private final MedicalFileRepository medicalFileRepository;
    private final LocalMedicalFileStorageService storageService;
    private final MedicalFileOcrTransactionService ocrTransactionService;
    private final MedicalFileOcrProcessingService ocrProcessingService;

    public MedicalFileService(MedicalFileRepository medicalFileRepository,
                              LocalMedicalFileStorageService storageService,
                              MedicalFileOcrTransactionService ocrTransactionService,
                              MedicalFileOcrProcessingService ocrProcessingService) {
        this.medicalFileRepository = medicalFileRepository;
        this.storageService = storageService;
        this.ocrTransactionService = ocrTransactionService;
        this.ocrProcessingService = ocrProcessingService;
    }

    @Transactional
    public MedicalFileUploadResponse upload(UserAccount patient, MedicalFileType type, MultipartFile file) {
        StoredMedicalFile storedFile = storageService.store(patient, type, file);
        try {
            MedicalFile medicalFile = new MedicalFile(
                    patient,
                    type,
                    storedFile.originalFileName(),
                    storedFile.storedFileName(),
                    storedFile.contentType(),
                    storedFile.fileSize(),
                    storedFile.storagePath(),
                    LocalDateTime.now()
            );
            MedicalFile saved = medicalFileRepository.save(medicalFile);
            if (type == MedicalFileType.CHECKUP_RESULT) {
                ocrTransactionService.createPending(saved);
                startOcrAfterCommit(saved.getId());
            }
            return MedicalFileUploadResponse.from(saved);
        } catch (RuntimeException exception) {
            storageService.deleteQuietly(storedFile.storagePath());
            throw exception;
        }
    }

    private void startOcrAfterCommit(Long medicalFileId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ocrProcessingService.processAsync(medicalFileId);
                }
            });
            return;
        }
        ocrProcessingService.processAsync(medicalFileId);
    }
}
