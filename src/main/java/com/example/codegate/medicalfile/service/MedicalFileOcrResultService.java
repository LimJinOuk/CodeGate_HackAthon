package com.example.codegate.medicalfile.service;

import com.example.codegate.medicalfile.dto.MedicalFileOcrResultResponse;
import com.example.codegate.medicalfile.entity.MedicalFile;
import com.example.codegate.medicalfile.entity.MedicalFileOcrResult;
import com.example.codegate.medicalfile.repository.MedicalFileOcrResultRepository;
import com.example.codegate.medicalfile.repository.MedicalFileRepository;
import com.example.codegate.medicalfile.support.MedicalFileErrors;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Reservation;
import com.example.codegate.reservation.repository.ReservationRepository;
import com.example.codegate.reservation.support.ReservationErrors;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalFileOcrResultService {

    private final MedicalFileRepository medicalFileRepository;
    private final MedicalFileOcrResultRepository ocrResultRepository;
    private final ReservationRepository reservationRepository;

    public MedicalFileOcrResultService(MedicalFileRepository medicalFileRepository,
                                       MedicalFileOcrResultRepository ocrResultRepository,
                                       ReservationRepository reservationRepository) {
        this.medicalFileRepository = medicalFileRepository;
        this.ocrResultRepository = ocrResultRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public MedicalFileOcrResultResponse findMine(UserAccount patient, Long medicalFileId) {
        MedicalFile medicalFile = medicalFileRepository.findByIdAndPatient(medicalFileId, patient)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        MedicalFileOcrResult result = ocrResultRepository.findByMedicalFile(medicalFile)
                .orElseThrow(MedicalFileErrors::ocrResultNotFound);
        return MedicalFileOcrResultResponse.from(result);
    }

    @Transactional(readOnly = true)
    public MedicalFileOcrResultResponse findForHospitalReservation(Hospital hospital,
                                                                   Long reservationId,
                                                                   Long medicalFileId) {
        Long patientId = requireHospitalReservationPatientId(hospital, reservationId);
        MedicalFile medicalFile = medicalFileRepository.findByIdAndPatient_Id(medicalFileId, patientId)
                .orElseThrow(MedicalFileErrors::medicalFileNotFound);
        MedicalFileOcrResult result = ocrResultRepository.findByMedicalFile(medicalFile)
                .orElseThrow(MedicalFileErrors::ocrResultNotFound);
        return MedicalFileOcrResultResponse.from(result);
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
