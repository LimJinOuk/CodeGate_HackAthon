package com.example.codegate.reservation.repository;

import com.example.codegate.reservation.domain.Reservation;
import com.example.codegate.reservation.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * 목록 조회 메서드에는 모두 {@code hospital} 엔티티 그래프를 건다.
     * {@code ReservationResponse} 가 병원 이름 / 주소를 읽기 때문에, 페치 조인이 없으면
     * 예약 1건당 병원 SELECT 가 한 번씩 더 나간다(N+1).
     */
    @EntityGraph(attributePaths = "hospital")
    Optional<Reservation> findById(Long id);

    @EntityGraph(attributePaths = "hospital")
    List<Reservation> findByPatient_IdOrderByReservationDateAscStartTimeAsc(Long patientId);

    @EntityGraph(attributePaths = "hospital")
    Page<Reservation> findByPatient_Id(Long patientId, Pageable pageable);

    @EntityGraph(attributePaths = "hospital")
    List<Reservation> findByPatient_IdAndStatusOrderByReservationDateAscStartTimeAsc(
            Long patientId, ReservationStatus status);

    @EntityGraph(attributePaths = "hospital")
    Page<Reservation> findByPatient_IdAndStatus(Long patientId, ReservationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "hospital")
    List<Reservation> findByHospital_IdOrderByReservationDateAscStartTimeAsc(Long hospitalId);

    @EntityGraph(attributePaths = "hospital")
    Page<Reservation> findByHospital_Id(Long hospitalId, Pageable pageable);

    @EntityGraph(attributePaths = "hospital")
    Page<Reservation> findByHospital_IdAndReservationDateBetween(
            Long hospitalId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    @EntityGraph(attributePaths = "hospital")
    List<Reservation> findByHospital_IdAndStatusOrderByReservationDateAscStartTimeAsc(
            Long hospitalId, ReservationStatus status);

    @EntityGraph(attributePaths = "hospital")
    Page<Reservation> findByHospital_IdAndStatus(Long hospitalId, ReservationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "hospital")
    Page<Reservation> findByHospital_IdAndStatusAndReservationDateBetween(
            Long hospitalId, ReservationStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /** 같은 사용자가 같은 시간대에 이미 진행 중(승인대기/확정)인 예약을 갖고 있는지 */
    boolean existsByPatient_IdAndReservationDateAndStartTimeAndStatusIn(
            Long patientId, LocalDate reservationDate, LocalTime startTime, Collection<ReservationStatus> statuses);

    /** 해당 시간대를 참조하는 예약이 하나라도 있는지 (시간대 삭제 가드) */
    boolean existsBySlot_Id(Long slotId);
}
