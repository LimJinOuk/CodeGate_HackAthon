package com.example.codegate.reservation.domain;

import com.example.codegate.hospital.entity.Hospital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 병원이 미리 등록해 둔 "진료 가능 시간대" 한 칸.
 *
 * <p><b>진료는 1시간 단위로 진행되고, 한 시간에 1명만 진료할 수 있다.</b>
 * 따라서 슬롯은 정원 개념 없이 "비어 있음 / 찼음" 두 상태만 가진다.
 * 시작 시각은 항상 정시(분 = 0)이고 종료 시각은 시작 + 1시간이다.</p>
 *
 * <p>{@code reserved} 는 승인 대기(REQUESTED)와 확정(APPROVED)을 모두 포함한다.
 * 사용자가 예약을 요청하는 순간 자리를 선점하고,
 * 병원이 거절하거나 사용자가 취소하면 자리를 반납한다.</p>
 */
@Entity
@Table(
        name = "schedule_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_slot_hospital_department_datetime",
                columnNames = {"hospital_id", "department", "slot_date", "start_time"}
        )
)
public class ScheduleSlot {

    /** 1회 진료 시간 (시간 단위) */
    public static final int DURATION_HOURS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Department department;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private boolean reserved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ScheduleSlot() {
    }

    public ScheduleSlot(Hospital hospital, Department department, LocalDate slotDate, LocalTime startTime) {
        this.hospital = hospital;
        this.department = department;
        this.slotDate = slotDate;
        this.startTime = startTime.withMinute(0).withSecond(0).withNano(0);
        this.reserved = false;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public Long getHospitalId() {
        return hospital.getId();
    }

    public Department getDepartment() {
        return department;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    /** 종료 시각은 항상 시작 + 1시간 */
    public LocalTime getEndTime() {
        return startTime.plusHours(DURATION_HOURS);
    }

    public boolean isReserved() {
        return reserved;
    }

    public LocalDateTime startsAt() {
        return LocalDateTime.of(slotDate, startTime);
    }

    public boolean isPast(LocalDateTime now) {
        return startsAt().isBefore(now);
    }

    /** 예약 가능 여부: 지나지 않았고 아직 비어 있어야 한다. */
    public boolean isAvailable(LocalDateTime now) {
        return !reserved && !isPast(now);
    }

    /** 자리 선점. 이미 예약된 시간대면 false. */
    public boolean reserve() {
        if (reserved) {
            return false;
        }
        reserved = true;
        return true;
    }

    /** 선점 반납(병원 거절 / 사용자 취소). */
    public void release() {
        reserved = false;
    }
}
