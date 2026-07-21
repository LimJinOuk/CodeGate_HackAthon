package com.example.codegate.reservation.domain;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.user.entity.UserAccount;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사용자의 예약 요청 1건. 병원이 승인해야 최종 확정된다.
 *
 * <p>진료과 / 날짜 / 시각은 슬롯에서 복사해 보관한다(스냅샷).
 * 예약은 이력 성격이라 나중에 슬롯 구성이 바뀌어도 당시 정보가 남아야 하기 때문이다.</p>
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private ScheduleSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    /** 예약자 (인증 모듈의 사용자 계정) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private UserAccount patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Department department;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(nullable = false, length = 20)
    private String patientName;

    @Column(nullable = false, length = 20)
    private String patientPhone;

    @Column(length = 200)
    private String symptom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime decidedAt;

    @Column(length = 200)
    private String decisionMessage;

    protected Reservation() {
    }

    public Reservation(ScheduleSlot slot, UserAccount patient,
                       String patientName, String patientPhone, String symptom,
                       LocalDateTime requestedAt) {
        this.slot = slot;
        this.hospital = slot.getHospital();
        this.patient = patient;
        this.department = slot.getDepartment();
        this.reservationDate = slot.getSlotDate();
        this.startTime = slot.getStartTime();
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.symptom = symptom;
        this.requestedAt = requestedAt;
        this.status = ReservationStatus.REQUESTED;
    }

    public Long getId() {
        return id;
    }

    public ScheduleSlot getSlot() {
        return slot;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public Long getHospitalId() {
        return hospital.getId();
    }

    public UserAccount getPatient() {
        return patient;
    }

    public Long getPatientId() {
        return patient.getId();
    }

    public Department getDepartment() {
        return department;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return startTime.plusHours(ScheduleSlot.DURATION_HOURS);
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public String getSymptom() {
        return symptom;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionMessage() {
        return decisionMessage;
    }

    public void approve(String message, LocalDateTime now) {
        this.status = ReservationStatus.APPROVED;
        this.decidedAt = now;
        this.decisionMessage = message;
    }

    public void reject(String reason, LocalDateTime now) {
        this.status = ReservationStatus.REJECTED;
        this.decidedAt = now;
        this.decisionMessage = reason;
    }

    public void cancel(LocalDateTime now) {
        this.status = ReservationStatus.CANCELED;
        this.decidedAt = now;
        this.decisionMessage = "사용자가 예약을 취소했습니다.";
    }
}
