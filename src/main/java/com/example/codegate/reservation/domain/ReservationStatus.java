package com.example.codegate.reservation.domain;

/**
 * 예약 상태 흐름
 *
 * <pre>
 *   REQUESTED ──(병원 승인)──▶ APPROVED
 *       │
 *       ├──(병원 거절)──▶ REJECTED
 *       └──(사용자 취소)──▶ CANCELED
 *
 *   APPROVED ──(사용자 취소)──▶ CANCELED
 * </pre>
 */
public enum ReservationStatus {

    REQUESTED("승인대기"),
    APPROVED("예약확정"),
    REJECTED("병원거절"),
    CANCELED("사용자취소");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == REQUESTED || this == APPROVED;
    }
}
