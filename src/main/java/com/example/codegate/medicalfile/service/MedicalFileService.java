package com.example.codegate.medicalfile.service;

import com.example.codegate.medicalfile.dto.MedicalFileContentResponse;
import com.example.codegate.medicalfile.dto.MedicalFileUploadResponse;
import com.example.codegate.medicalfile.dto.MedicalFileResponse;
import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileType;
import com.example.codegate.medicalfile.repository.MedicalFileRepository;
import com.example.codegate.medicalfile.support.MedicalFileErrors;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Reservation;
import com.example.codegate.reservation.repository.ReservationRepository;
import com.example.codegate.reservation.support.ReservationErrors;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicalFileService {

    private final MedicalFileRepository medicalFileRepository;
    private final ReservationRepository reservationRepository;
    private final LocalMedicalFileStorageService storageService;
    private final MedicalFileOcrTransactionService ocrTransactionService;
    private final MedicalFileOcrProcessingService ocrProcessingService;

    public MedicalFileService(MedicalFileRepository medicalFileRepository,
                              ReservationRepository reservationRepository,
                              LocalMedicalFileStorageService storageService,
                              MedicalFileOcrTransactionService ocrTransactionService,
                              MedicalFileOcrProcessingService ocrProcessingService) {
        this.medicalFileRepository = medicalFileRepository;
        this.reservationRepository = reservationRepository;
        this.storageService = storageService;
        this.ocrTransactionService = ocrTransactionService;
        this.ocrProcessingService = ocrProcessingService;
    }

    @Transactional(readOnly = true)
    public List<MedicalFileResponse> findMine(UserAccount patient) {
        return medicalFileRepository.findByPatientOrderByCreatedAtDesc(patient)
                .stream()
                .map(MedicalFileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicalFileContentResponse findContentMine(UserAccount patient, Long medicalFileId) {
        MedicalFile medicalFile = medicalFileRepository.findByIdAndPatient(medicalFileId, patient)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        PathResource resource = new PathResource(storageService.resolveForRead(medicalFile.getStoragePath()));
        if (!resource.exists() || !resource.isReadable()) {
            throw MedicalFileErrors.medicalFileNotFound();
        }

        return new MedicalFileContentResponse(
                medicalFile.getOriginalFileName(),
                medicalFile.getContentType(),
                medicalFile.getFileSize(),
                resource
        );
    }

    @Transactional(readOnly = true)
    public List<MedicalFileResponse> findForHospitalReservation(Hospital hospital, Long reservationId) {
        Long patientId = requireHospitalReservationPatientId(hospital, reservationId);
        return medicalFileRepository.findByPatient_IdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(MedicalFileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicalFileContentResponse findContentForHospitalReservation(Hospital hospital,
                                                                        Long reservationId,
                                                                        Long medicalFileId) {
        Long patientId = requireHospitalReservationPatientId(hospital, reservationId);
        MedicalFile medicalFile = medicalFileRepository.findByIdAndPatient_Id(medicalFileId, patientId)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        PathResource resource = new PathResource(storageService.resolveForRead(medicalFile.getStoragePath()));
        if (!resource.exists() || !resource.isReadable()) {
            throw MedicalFileErrors.medicalFileNotFound();
        }

        return new MedicalFileContentResponse(
                medicalFile.getOriginalFileName(),
                medicalFile.getContentType(),
                medicalFile.getFileSize(),
                resource
        );
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

    private Long requireHospitalReservationPatientId(Hospital hospital, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrors::reservationNotFound);
        if (!reservation.getHospitalId().equals(hospital.getId())) {
            throw ReservationErrors.notOwnHospitalReservation();
        }
        return reservation.getPatientId();
    }
}
