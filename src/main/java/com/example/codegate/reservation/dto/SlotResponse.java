package com.example.codegate.reservation.dto;

import com.example.codegate.reservation.domain.ScheduleSlot;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 예약 가능한 진료 시간대 한 칸 (캘린더 UI 의 셀에 해당).
 * 진료는 1시간 단위이며 한 시간에 1명만 예약할 수 있다.
 */
public record SlotResponse(
        Long slotId,
        String department,
        LocalDate date,
        String dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        boolean reserved,
        boolean available
) {
    public static SlotResponse from(ScheduleSlot slot, LocalDateTime now) {
        return new SlotResponse(
                slot.getId(),
                slot.getDepartment().getLabel(),
                slot.getSlotDate(),
                slot.getSlotDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.isReserved(),
                slot.isAvailable(now)
        );
    }
}
