package com.example.codegate.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 병원이 진료 가능 시간대를 등록할 때 사용.
 *
 * <p>진료는 1시간 단위이므로 시작 시각(정시)만 받고 종료 시각은 시작 + 1시간으로 자동 계산된다.
 * 한 시간에 1명만 진료하므로 정원 입력도 없다.</p>
 */
public record SlotCreateRequest(

        @NotBlank(message = "진료과목은 필수입니다.")
        String department,

        @NotNull(message = "날짜는 필수입니다.")
        LocalDate date,

        @NotNull(message = "시작 시간은 필수입니다. (정시만 가능, 예: 09:00)")
        @JsonFormat(pattern = "HH:mm") LocalTime startTime
) {
}
