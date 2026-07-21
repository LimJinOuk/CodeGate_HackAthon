package com.example.codegate.reservation.repository;

import com.example.codegate.reservation.domain.Reservation;
import com.example.codegate.reservation.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 연관관계를 타는 조건은 {@code Patient_Id} 처럼 밑줄로 경로를 명시한다.
 * 밑줄이 없으면 Spring Data 가 엔티티의 {@code getPatientId()} 게터를 속성으로 오인해
 * {@code r.patientId} 라는 존재하지 않는 경로를 만들어낸다.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :reservationId")
    Optional<Reservation> findByIdForUpdate(@Param("reservationId") Long reservationId);

    List<Reservation> findByPatient_IdOrderByReservationDateAscStartTimeAsc(Long patientId);

    Page<Reservation> findByPatient_Id(Long patientId, Pageable pageable);

    List<Reservation> findByPatient_IdAndStatusOrderByReservationDateAscStartTimeAsc(
            Long patientId, ReservationStatus status);

    Page<Reservation> findByPatient_IdAndStatus(Long patientId, ReservationStatus status, Pageable pageable);

    List<Reservation> findByHospital_IdOrderByReservationDateAscStartTimeAsc(Long hospitalId);

    Page<Reservation> findByHospital_Id(Long hospitalId, Pageable pageable);

    Page<Reservation> findByHospital_IdAndReservationDateBetween(
            Long hospitalId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    List<Reservation> findByHospital_IdAndStatusOrderByReservationDateAscStartTimeAsc(
            Long hospitalId, ReservationStatus status);

    Page<Reservation> findByHospital_IdAndStatus(Long hospitalId, ReservationStatus status, Pageable pageable);

    Page<Reservation> findByHospital_IdAndStatusAndReservationDateBetween(
            Long hospitalId, ReservationStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /** 같은 사용자가 같은 시간대에 이미 진행 중(승인대기/확정)인 예약을 갖고 있는지 */
    boolean existsByPatient_IdAndReservationDateAndStartTimeAndStatusIn(
            Long patientId, LocalDate reservationDate, LocalTime startTime, Collection<ReservationStatus> statuses);

    /** 해당 시간대를 참조하는 예약이 하나라도 있는지 (시간대 삭제 가드) */
    boolean existsBySlot_Id(Long slotId);
}
