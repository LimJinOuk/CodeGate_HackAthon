package com.example.codegate.reservation.service;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Reservation;
import com.example.codegate.reservation.domain.ReservationStatus;
import com.example.codegate.reservation.domain.ScheduleSlot;
import com.example.codegate.reservation.dto.PatientMedicalInfoResponse;
import com.example.codegate.reservation.dto.ReservationCreateRequest;
import com.example.codegate.reservation.dto.ReservationResponse;
import com.example.codegate.reservation.repository.ReservationRepository;
import com.example.codegate.reservation.repository.ScheduleSlotRepository;
import com.example.codegate.reservation.support.ReservationErrors;
import com.example.codegate.user.entity.PatientProfile;
import com.example.codegate.user.entity.UserAccount;
import com.example.codegate.user.repository.PatientProfileRepository;
import com.example.codegate.user.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
    private final PatientProfileRepository patientProfileRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ScheduleSlotRepository slotRepository,
                              UserAccountRepository userAccountRepository,
                              PatientProfileRepository patientProfileRepository) {
        this.reservationRepository = reservationRepository;
        this.slotRepository = slotRepository;
        this.userAccountRepository = userAccountRepository;
        this.patientProfileRepository = patientProfileRepository;
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

    /** 사용자의 예약 목록 (페이지네이션) */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> findMine(UserAccount patient, ReservationStatus status, Pageable pageable) {
        Page<Reservation> reservations = (status == null)
                ? reservationRepository.findByPatient_Id(patient.getId(), pageable)
                : reservationRepository.findByPatient_IdAndStatus(patient.getId(), status, pageable);
        return reservations.map(ReservationResponse::from);
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
        LocalDateTime now = LocalDateTime.now();
        requireCancelableBeforeStart(reservation, now);
        reservation.cancelByPatient("사용자가 예약을 취소했습니다.", now);
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

    /** 병원이 받은 예약 요청 목록 (페이지네이션). */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> findForHospital(Hospital hospital, ReservationStatus status, Pageable pageable) {
        Page<Reservation> reservations = (status == null)
                ? reservationRepository.findByHospital_Id(hospital.getId(), pageable)
                : reservationRepository.findByHospital_IdAndStatus(hospital.getId(), status, pageable);
        return reservations.map(ReservationResponse::from);
    }

    /** 병원이 받은 예약 요청 목록 (페이지네이션 + 날짜 범위). */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> findForHospital(Hospital hospital, ReservationStatus status,
                                                     LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate == null && toDate == null) {
            return findForHospital(hospital, status, pageable);
        }

        LocalDate from = fromDate == null ? LocalDate.of(1900, 1, 1) : fromDate;
        LocalDate to = toDate == null ? LocalDate.of(9999, 12, 31) : toDate;
        if (to.isBefore(from)) {
            throw ReservationErrors.invalidDateRange();
        }

        Page<Reservation> reservations = (status == null)
                ? reservationRepository.findByHospital_IdAndReservationDateBetween(hospital.getId(), from, to, pageable)
                : reservationRepository.findByHospital_IdAndStatusAndReservationDateBetween(
                        hospital.getId(), status, from, to, pageable);
        return reservations.map(ReservationResponse::from);
    }

    /**
     * 예약 건의 환자 의료 정보(복용약 / 질병) 조회.
     *
     * <p>회원가입 전에 만들어진 계정처럼 프로필이 없는 경우도 있어 빈 값으로 응답한다.
     * 여기서 예외를 던지면 병원이 예약 자체를 처리하지 못하게 된다.</p>
     */
    @Transactional(readOnly = true)
    public PatientMedicalInfoResponse findPatientMedicalInfo(Hospital hospital, Long reservationId) {
        Reservation reservation = getHospitalReservation(hospital, reservationId);
        PatientProfile profile = patientProfileRepository.findByUserAccount(reservation.getPatient())
                .orElse(null);
        return PatientMedicalInfoResponse.from(reservation, profile);
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

    /** 병원 취소. 승인 대기 / 확정 상태 모두 취소 가능하며 슬롯 자리를 반납한다. */
    @Transactional
    public ReservationResponse cancelByHospital(Hospital hospital, Long reservationId, String reason) {
        Reservation reservation = getHospitalReservationForUpdate(hospital, reservationId);
        if (!reservation.getStatus().isActive()) {
            throw ReservationErrors.reservationAlreadyClosed(reservation.getStatus().getLabel());
        }
        LocalDateTime now = LocalDateTime.now();
        requireCancelableBeforeStart(reservation, now);

        String text = (reason == null || reason.isBlank())
                ? "병원 사정으로 예약이 취소되었습니다."
                : reason;
        reservation.cancelByHospital(text, now);
        reservation.getSlot().release();
        return ReservationResponse.from(reservation);
    }

    // ------------------------------------------------------------------ 내부용

    private void requirePending(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.REQUESTED) {
            throw ReservationErrors.reservationNotPending(reservation.getStatus().getLabel());
        }
    }

    private void requireCancelableBeforeStart(Reservation reservation, LocalDateTime now) {
        if (!reservation.startsAt().isAfter(now)) {
            throw ReservationErrors.reservationAlreadyStarted();
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
