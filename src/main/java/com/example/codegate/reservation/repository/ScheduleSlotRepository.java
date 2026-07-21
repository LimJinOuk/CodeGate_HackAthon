package com.example.codegate.reservation.repository;

import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.ScheduleSlot;
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

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    // 연관관계 경로는 밑줄로 명시한다 (getHospitalId() 게터와 혼동되지 않도록)
    List<ScheduleSlot> findByHospital_IdOrderBySlotDateAscStartTimeAsc(Long hospitalId);

    Page<ScheduleSlot> findByHospital_Id(Long hospitalId, Pageable pageable);

    Page<ScheduleSlot> findByHospital_IdAndSlotDate(Long hospitalId, LocalDate slotDate, Pageable pageable);

    Page<ScheduleSlot> findByHospital_IdAndDepartment(Long hospitalId, Department department, Pageable pageable);

    Page<ScheduleSlot> findByHospital_IdAndSlotDateAndDepartment(
            Long hospitalId, LocalDate slotDate, Department department, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScheduleSlot s where s.id = :slotId")
    Optional<ScheduleSlot> findByIdForUpdate(@Param("slotId") Long slotId);

    boolean existsByHospital_IdAndSlotDateAndStartTime(Long hospitalId, LocalDate slotDate, LocalTime startTime);

    /**
     * 예약 조회용. 아직 예약되지 않았고 지나지 않은 시간대만 가져온다.
     * 날짜 / 시간 범위는 파라미터가 null 이면 조건에서 제외된다.
     */
    @Query("""
            select s from ScheduleSlot s
            join fetch s.hospital h
            where h.id in :hospitalIds
              and s.reserved = false
              and (s.slotDate > :today or (s.slotDate = :today and s.startTime >= :nowTime))
              and (:date is null or s.slotDate = :date)
              and (:fromTime is null or s.startTime >= :fromTime)
              and (:toTime is null or s.startTime <= :toTime)
            order by s.slotDate asc, s.startTime asc, h.id asc
            """)
    List<ScheduleSlot> findAvailableSlots(
            @Param("hospitalIds") Collection<Long> hospitalIds,
            @Param("today") LocalDate today,
            @Param("nowTime") LocalTime nowTime,
            @Param("date") LocalDate date,
            @Param("fromTime") LocalTime fromTime,
            @Param("toTime") LocalTime toTime);
}
