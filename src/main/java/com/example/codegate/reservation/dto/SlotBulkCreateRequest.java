package com.example.codegate.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 병원이 같은 날짜/진료과에 여러 시작 시각을 한 번에 등록할 때 사용한다. */
public record SlotBulkCreateRequest(

        @NotBlank(message = "진료과목은 필수입니다.")
        String department,

        @NotNull(message = "날짜는 필수입니다.")
        LocalDate date,

        @NotEmpty(message = "시작 시간 목록은 하나 이상 필요합니다.")
        List<@NotNull(message = "시작 시간은 필수입니다.") LocalTime> starts
) {
    public List<SlotCreateRequest> toSlotCreateRequests() {
        return starts.stream()
                .map(start -> new SlotCreateRequest(department, date, start))
                .toList();
    }
}
