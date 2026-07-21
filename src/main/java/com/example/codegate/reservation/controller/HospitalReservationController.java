package com.example.codegate.reservation.controller;

import com.example.codegate.global.ApiResponse;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.dto.DecisionRequest;
import com.example.codegate.reservation.dto.PatientMedicalInfoResponse;
import com.example.codegate.reservation.dto.ReservationResponse;
import com.example.codegate.reservation.dto.SlotBulkCreateRequest;
import com.example.codegate.reservation.dto.SlotCreateRequest;
import com.example.codegate.reservation.dto.SlotResponse;
import com.example.codegate.reservation.service.HospitalScheduleService;
import com.example.codegate.reservation.service.ReservationService;
import com.example.codegate.reservation.support.CallerResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 병원용 예약 API - 진료 가능 시간대 관리 + 예약 요청 승인/거절.
 * {@code Authorization: Bearer {accessToken}} 헤더가 필요하다.
 */
@RestController
@RequestMapping({"/api/v1/hospital", "/api/v1/hospitals/me"})
public class HospitalReservationController {

    private final CallerResolver callerResolver;
    private final HospitalScheduleService scheduleService;
    private final ReservationService reservationService;

    public HospitalReservationController(CallerResolver callerResolver,
                                         HospitalScheduleService scheduleService,
                                         ReservationService reservationService) {
        this.callerResolver = callerResolver;
        this.scheduleService = scheduleService;
        this.reservationService = reservationService;
    }

    // ------------------------------------------------------- 진료 가능 시간대 관리

    /** 우리 병원이 등록해 둔 진료 가능 시간대 조회 */
    @GetMapping("/slots")
    public ApiResponse<Page<SlotResponse>> slots(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(scheduleService.findSlots(hospital, date, department, pageRequest(page, size,
                Sort.by("slotDate").ascending().and(Sort.by("startTime").ascending()))));
    }

    /** 진료 가능 시간대 추가 등록 (1시간 단위, 시작 시각은 정시) */
    @PostMapping("/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SlotResponse> addSlot(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody SlotCreateRequest request) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(scheduleService.addSlot(hospital, request));
    }

    /** 진료 가능 시간대 일괄 등록 */
    @PostMapping("/slots/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<SlotResponse>> addSlots(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody SlotBulkCreateRequest request) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(scheduleService.addSlots(hospital, request.toSlotCreateRequests()));
    }

    /** 진료 가능 시간대 삭제 */
    @DeleteMapping("/slots/{slotId}")
    public ApiResponse<Void> deleteSlot(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long slotId) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        scheduleService.deleteSlot(hospital, slotId);
        return ApiResponse.ok(null);
    }

    // ------------------------------------------------------------- 예약 요청 처리

    /** 접수된 예약 요청 목록 (status=REQUESTED 로 승인 대기 건만 조회) */
    @GetMapping("/reservations")
    public ApiResponse<Page<ReservationResponse>> reservations(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(reservationService.findForHospital(
                hospital,
                PatientReservationController.parseStatus(status),
                fromDate,
                toDate,
                pageRequest(page, size, Sort.by("reservationDate").ascending().and(Sort.by("startTime").ascending()))));
    }

    /** 예약 환자의 의료 정보 (복용 중인 약 / 앓고 있는 질병) */
    @GetMapping("/reservations/{reservationId}/patient")
    public ApiResponse<PatientMedicalInfoResponse> patientMedicalInfo(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(reservationService.findPatientMedicalInfo(hospital, reservationId));
    }

    /** 예약 승인 → 최종 확정 */
    @PostMapping("/reservations/{reservationId}/approve")
    public ApiResponse<ReservationResponse> approve(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId,
            @Valid @RequestBody(required = false) DecisionRequest request) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(reservationService.approve(hospital, reservationId,
                request == null ? null : request.message()));
    }

    /** 예약 거절 → 선점했던 자리 반납 */
    @PostMapping("/reservations/{reservationId}/reject")
    public ApiResponse<ReservationResponse> reject(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId,
            @Valid @RequestBody(required = false) DecisionRequest request) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(reservationService.reject(hospital, reservationId,
                request == null ? null : request.message()));
    }

    /** 병원 취소 → 선점했던 자리 반납 */
    @PostMapping("/reservations/{reservationId}/cancel")
    public ApiResponse<ReservationResponse> cancelByHospital(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long reservationId,
            @Valid @RequestBody(required = false) DecisionRequest request) {
        Hospital hospital = callerResolver.requireHospital(authorizationHeader);
        return ApiResponse.ok(reservationService.cancelByHospital(hospital, reservationId,
                request == null ? null : request.message()));
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }
}
