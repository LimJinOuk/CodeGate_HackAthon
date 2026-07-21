package com.example.codegate.reservation.dto;

import jakarta.validation.constraints.Size;

/** 병원의 승인/거절 시 함께 보낼 안내 메시지(선택) */
public record DecisionRequest(
        @Size(max = 200, message = "메시지는 200자를 넘을 수 없습니다.")
        String message
) {
}
