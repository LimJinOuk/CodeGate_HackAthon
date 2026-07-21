package com.example.codegate.reservation.service;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Reservation;
import com.example.codegate.reservation.domain.ReservationStatus;
import com.example.codegate.reservation.domain.ScheduleSlot;
import com.example.codegate.reservation.dto.ReservationCreateRequest;
import com.example.codegate.reservation.dto.ReservationResponse;
import com.example.codegate.reservation.repository.ReservationRepository;
import com.example.codegate.reservation.repository.ScheduleSlotRepository;
import com.example.codegate.reservation.support.ReservationErrors;
import com.example.codegate.user.entity.UserAccount;
import com.example.codegate.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 예약 요청 → 병원 승인/거절 → 확정 흐름을 담당한다.
 */
@Service
public class ReservationService {

    private static final Set<ReservationStatus> ACTIVE_STATUSES =
            Set.of(ReservationStatus.REQUESTED, ReservationStatus.APPROVED);

    private final ReservationRepository reservationRepository;
    private final ScheduleSlotRepository slotRepository;
    private final UserAccountRepository userAccountRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ScheduleSlotRepository slotRepository,
                              UserAccountRepository userAccountRepository) {
        this.reservationRepository = reservationRepository;
        this.slotRepository = slotRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // ------------------------------------------------------------------ 사용자

    /** 예약 요청. 성공하면 해당 1시간 슬롯을 선점하고 REQUESTED 상태로 생성된다. */
    @Transactional
    public ReservationResponse request(UserAccount patient, ReservationCreateRequest request) {
        UserAccount lockedPatient = userAccountRepository.findByIdForUpdate(patient.getId())
                .orElseThrow(ReservationErrors::userNotFound);
        ScheduleSlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> ReservationErrors.slotNotFound(request.slotId()));

        LocalDateTime now = LocalDateTime.now();
        if (slot.isPast(now)) {
            throw ReservationErrors.slotPast();
        }
        if (reservationRepository.existsByPatient_IdAndReservationDateAndStartTimeAndStatusIn(
                lockedPatient.getId(), slot.getSlotDate(), slot.getStartTime(), ACTIVE_STATUSES)) {
            throw ReservationErrors.duplicateReservation();
        }
        if (!slot.reserve()) {
            throw ReservationErrors.slotAlreadyTaken();
        }

        Reservation reservation = new Reservation(
                slot,
                lockedPatient,
                request.patientName(),
                request.patientPhone(),
                request.symptom(),
                now
        );
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    /** 사용자의 예약 목록 */
    @Transactional(readOnly = true)
    public List<ReservationResponse> findMine(UserAccount patient, ReservationStatus status) {
        List<Reservation> reservations = (status == null)
                ? reservationRepository.findByPatient_IdOrderByReservationDateAscStartTimeAsc(patient.getId())
                : reservationRepository.findByPatient_IdAndStatusOrderByReservationDateAscStartTimeAsc(
                        patient.getId(), status);
        return reservations.stream().map(ReservationResponse::from).toList();
    }

    /** 예약 단건 조회 (본인 것만) */
    @Transactional(readOnly = true)
    public ReservationResponse findMineById(UserAccount patient, Long reservationId) {
        return ReservationResponse.from(getOwnReservation(patient, reservationId));
    }

    /** 사용자 취소. 승인 대기 / 확정 상태 모두 취소 가능하며 슬롯 자리를 반납한다. */
    @Transactional
    public ReservationResponse cancel(UserAccount patient, Long reservationId) {
        Reservation reservation = getOwnReservationForUpdate(patient, reservationId);
        if (!reservation.getStatus().isActive()) {
            throw ReservationErrors.reservationAlreadyClosed(reservation.getStatus().getLabel());
        }
        reservation.cancel(LocalDateTime.now());
        reservation.getSlot().release();
        return ReservationResponse.from(reservation);
    }

    // -------------------------------------------------------------------- 병원

    /** 병원이 받은 예약 요청 목록. status 를 주지 않으면 전체. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> findForHospital(Hospital hospital, ReservationStatus status) {
        List<Reservation> reservations = (status == null)
                ? reservationRepository.findByHospital_IdOrderByReservationDateAscStartTimeAsc(hospital.getId())
                : reservationRepository.findByHospital_IdAndStatusOrderByReservationDateAscStartTimeAsc(
                        hospital.getId(), status);
        return reservations.stream().map(ReservationResponse::from).toList();
    }

    /** 병원 승인 → 예약 최종 확정 */
    @Transactional
    public ReservationResponse approve(Hospital hospital, Long reservationId, String message) {
        Reservation reservation = getHospitalReservationForUpdate(hospital, reservationId);
        requirePending(reservation);

        String text = (message == null || message.isBlank())
                ? "예약이 확정되었습니다. 예약 시간 10분 전까지 내원해 주세요."
                : message;
        reservation.approve(text, LocalDateTime.now());
        return ReservationResponse.from(reservation);
    }

    /** 병원 거절 → 선점했던 슬롯 자리 반납 */
    @Transactional
    public ReservationResponse reject(Hospital hospital, Long reservationId, String reason) {
        Reservation reservation = getHospitalReservationForUpdate(hospital, reservationId);
        requirePending(reservation);

        String text = (reason == null || reason.isBlank())
                ? "해당 시간대는 진료가 어렵습니다. 다른 시간대를 선택해 주세요."
                : reason;
        reservation.reject(text, LocalDateTime.now());
        reservation.getSlot().release();
        return ReservationResponse.from(reservation);
    }

    // ------------------------------------------------------------------ 내부용

    private void requirePending(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.REQUESTED) {
            throw ReservationErrors.reservationNotPending(reservation.getStatus().getLabel());
        }
    }

    private Reservation getOwnReservation(UserAccount patient, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrors::reservationNotFound);
        if (!reservation.getPatientId().equals(patient.getId())) {
            throw ReservationErrors.notOwnReservation();
        }
        return reservation;
    }

    private Reservation getOwnReservationForUpdate(UserAccount patient, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(ReservationErrors::reservationNotFound);
        if (!reservation.getPatientId().equals(patient.getId())) {
            throw ReservationErrors.notOwnReservation();
        }
        return reservation;
    }

    private Reservation getHospitalReservation(Hospital hospital, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrors::reservationNotFound);
        if (!reservation.getHospitalId().equals(hospital.getId())) {
            throw ReservationErrors.notOwnHospitalReservation();
        }
        return reservation;
    }

    private Reservation getHospitalReservationForUpdate(Hospital hospital, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(ReservationErrors::reservationNotFound);
        if (!reservation.getHospitalId().equals(hospital.getId())) {
            throw ReservationErrors.notOwnHospitalReservation();
        }
        return reservation;
    }
}
