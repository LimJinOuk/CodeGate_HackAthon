package com.example.codegate.reservation.controller;

import com.example.codegate.global.ApiResponse;
import com.example.codegate.reservation.domain.ReservationStatus;
import com.example.codegate.reservation.dto.ReservationCreateRequest;
import com.example.codegate.reservation.dto.ReservationResponse;
import com.example.codegate.reservation.service.ReservationService;
import com.example.codegate.reservation.support.CallerResolver;
import com.example.codegate.reservation.support.ReservationErrors;
import com.example.codegate.user.entity.UserAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 서비스 이용자(PATIENT)용 예약 API.
 * {@code Authorization: Bearer {accessToken}} 헤더가 필요하다.
 */
@RestController
@RequestMapping("/api/v1/patient/reservations")
public class PatientReservationController {

    private final CallerResolver callerResolver;
    private final ReservationService reservationService;

    public PatientReservationController(CallerResolver callerResolver, ReservationService reservationService) {
        this.callerResolver = callerResolver;
        this.reservationService = reservationService;
    }

    /** 예약 요청 (병원 승인 전까지 '승인대기' 상태) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReservationResponse> request(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ReservationCreateRequest request) {
        UserAccount patient = callerResolver.requirePatient(authorizationHeader);
        return ApiResponse.ok(reservationService.request(patient, request));
    }

    /** 내 예약 목록 */
    @GetMapping
    public ApiResponse<List<ReservationResponse>> myReservations(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) String status) {
        UserAccount patient = callerResolver.requirePatient(authorizationHeader);
        return ApiResponse.ok(reservationService.findMine(patient, parseStatus(status)));
    }

    /** 내 예약 단건 조회 (승인 여부 확인) */
    @GetMapping("/{reservationId}")
    public ApiResponse<ReservationResponse> detail(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId) {
        UserAccount patient = callerResolver.requirePatient(authorizationHeader);
        return ApiResponse.ok(reservationService.findMineById(patient, reservationId));
    }

    /** 예약 취소 */
    @PostMapping("/{reservationId}/cancel")
    public ApiResponse<ReservationResponse> cancel(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId) {
        UserAccount patient = callerResolver.requirePatient(authorizationHeader);
        return ApiResponse.ok(reservationService.cancel(patient, reservationId));
    }

    static ReservationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ReservationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ReservationErrors.invalidStatus(value);
        }
    }
}
