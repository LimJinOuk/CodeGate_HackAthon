package com.example.codegate.auth.dto;

import com.example.codegate.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PatientKakaoSignupRequest(
        String code,
        String redirectUri,
        String signupToken,
        @NotBlank @Size(max = 50) String name,
        @NotNull Gender gender,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Pattern(regexp = "^\\d{6}-?\\d{7}$", message = "주민등록번호는 13자리 숫자 또는 hyphen 포함 형식이어야 합니다.")
        String residentRegistrationNumber,

        /** 현재 복용 중인 약. 선택 항목이라 생략하거나 빈 배열로 보낼 수 있다. */
        @Size(max = 30) List<@Size(max = 100) String> medications,

        /** 앓고 있는 질병. 선택 항목이라 생략하거나 빈 배열로 보낼 수 있다. */
        @Size(max = 30) List<@Size(max = 100) String> diseases
) {
}
