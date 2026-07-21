package com.example.codegate.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReservationCreateRequest(

        @NotNull(message = "예약할 진료 시간대(slotId)는 필수입니다.")
        Long slotId,

        @NotBlank(message = "수진자 이름은 필수입니다.")
        @Size(max = 20, message = "이름은 20자를 넘을 수 없습니다.")
        String patientName,

        @NotBlank(message = "연락처는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "연락처는 010-1234-5678 형식이어야 합니다.")
        String patientPhone,

        @Size(max = 200, message = "요청사항은 200자를 넘을 수 없습니다.")
        String symptom
) {
}
