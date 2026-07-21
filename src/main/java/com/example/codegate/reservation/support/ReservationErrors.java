package com.example.codegate.reservation.support;

import com.example.codegate.global.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 예약 모듈이 사용하는 오류 코드 모음.
 *
 * <p>인증 모듈과 동일하게 {@link BusinessException} 을 던지며,
 * 응답은 전역 {@code GlobalExceptionHandler} 가 {@code {success,data,error}} 형식으로 변환한다.</p>
 */
public final class ReservationErrors {

    private ReservationErrors() {
    }

    // ------------------------------------------------------------------ 검색

    public static BusinessException districtRequired() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "DISTRICT_REQUIRED",
                "지역구(district)는 필수 선택 항목입니다.");
    }

    public static BusinessException unknownDistrict(String value) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "UNKNOWN_DISTRICT",
                "존재하지 않는 지역구입니다: " + value);
    }

    public static BusinessException unknownDepartment(String value) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "UNKNOWN_DEPARTMENT",
                "존재하지 않는 진료과목입니다: " + value);
    }

    public static BusinessException invalidTimeRange() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE",
                "종료 시간은 시작 시간보다 늦어야 합니다.");
    }

    public static BusinessException invalidStatus(String value) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "status 는 REQUESTED, APPROVED, REJECTED, CANCELED 중 하나여야 합니다. (입력값: " + value + ")");
    }

    // ------------------------------------------------------------------ 예약

    public static BusinessException slotNotFound(Long slotId) {
        return new BusinessException(HttpStatus.NOT_FOUND, "SLOT_NOT_FOUND",
                "존재하지 않는 진료 시간대입니다: " + slotId);
    }

    public static BusinessException slotPast() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "SLOT_PAST",
                "이미 지난 시간대는 예약할 수 없습니다.");
    }

    public static BusinessException slotAlreadyTaken() {
        return new BusinessException(HttpStatus.CONFLICT, "SLOT_ALREADY_TAKEN",
                "해당 시간대는 이미 다른 예약이 잡혀 있습니다.");
    }

    public static BusinessException duplicateReservation() {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_RESERVATION",
                "동일한 시간대에 이미 진행 중인 예약이 있습니다.");
    }

    public static BusinessException reservationNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND",
                "존재하지 않는 예약입니다.");
    }

    public static BusinessException reservationNotPending(String currentLabel) {
        return new BusinessException(HttpStatus.CONFLICT, "RESERVATION_NOT_PENDING",
                "승인 대기 상태의 예약만 처리할 수 있습니다. (현재 상태: " + currentLabel + ")");
    }

    public static BusinessException reservationAlreadyClosed(String currentLabel) {
        return new BusinessException(HttpStatus.CONFLICT, "RESERVATION_ALREADY_CLOSED",
                "이미 " + currentLabel + " 상태인 예약입니다.");
    }

    public static BusinessException notOwnReservation() {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "본인의 예약만 조회/취소할 수 있습니다.");
    }

    public static BusinessException notOwnHospitalReservation() {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "우리 병원으로 접수된 예약이 아닙니다.");
    }

    // -------------------------------------------------------------- 시간대 관리

    public static BusinessException departmentNotSupported(String hospitalName, String department) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "DEPARTMENT_NOT_SUPPORTED",
                hospitalName + " 은(는) " + department + " 를 진료 항목으로 등록하지 않았습니다.");
    }

    public static BusinessException slotNotOnTheHour(Object startTime) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "SLOT_NOT_ON_THE_HOUR",
                "진료는 1시간 단위로 진행되므로 시작 시간은 정시여야 합니다. (입력값: " + startTime + ")");
    }

    public static BusinessException slotDuplicated(String detail) {
        return new BusinessException(HttpStatus.CONFLICT, "SLOT_DUPLICATED",
                detail + " 시간대는 이미 등록되어 있습니다.");
    }

    public static BusinessException slotHasReservation() {
        return new BusinessException(HttpStatus.CONFLICT, "SLOT_HAS_RESERVATION",
                "예약 이력이 있어 시간대를 삭제할 수 없습니다.");
    }

    public static BusinessException notOwnSlot() {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "우리 병원의 시간대가 아닙니다.");
    }

    // ------------------------------------------------------------------ 계정

    public static BusinessException patientRoleRequired() {
        return new BusinessException(HttpStatus.FORBIDDEN, "PATIENT_ROLE_REQUIRED",
                "일반 사용자만 접근할 수 있습니다.");
    }

    public static BusinessException hospitalRoleRequired() {
        return new BusinessException(HttpStatus.FORBIDDEN, "HOSPITAL_ROLE_REQUIRED",
                "병원 회원만 접근할 수 있습니다.");
    }

    public static BusinessException userNotFound() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND",
                "사용자 정보를 찾을 수 없습니다.");
    }

    public static BusinessException hospitalNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND",
                "병원 정보를 찾을 수 없습니다.");
    }
}
