package com.example.codegate.reservation.dto;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Reservation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationResponse(
        Long reservationId,
        String status,
        String statusLabel,
        Long slotId,
        Long hospitalId,
        String hospitalName,
        String hospitalLocation,
        String department,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Long patientId,
        String patientName,
        String patientPhone,
        String symptom,
        String timeZone,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        LocalDateTime canceledAt,
        LocalDateTime statusChangedAt,
        String hospitalMemo,
        String cancelReason
) {
    public static ReservationResponse from(Reservation r) {
        Hospital hospital = r.getHospital();
        return new ReservationResponse(
                r.getId(),
                r.getStatus().name(),
                r.getStatus().getLabel(),
                r.getSlot().getId(),
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalLocation(),
                r.getDepartment().getLabel(),
                r.getReservationDate(),
                r.getStartTime(),
                r.getEndTime(),
                r.getPatientId(),
                r.getPatientName(),
                r.getPatientPhone(),
                r.getSymptom(),
                "Asia/Seoul",
                r.getRequestedAt(),
                r.getApprovedAt(),
                r.getRejectedAt(),
                r.getCanceledAt(),
                r.getStatusChangedAt(),
                r.getHospitalMemo(),
                r.getCancelReason()
        );
    }
}
