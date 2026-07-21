package com.example.codegate.auth.dto;

import com.example.codegate.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientKakaoSignupRequest(
        @NotBlank String code,
        @NotBlank String redirectUri,
        @NotBlank @Size(max = 50) String name,
        @NotNull Gender gender,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Pattern(regexp = "^\\d{6}-?\\d{7}$", message = "주민등록번호는 13자리 숫자 또는 hyphen 포함 형식이어야 합니다.")
        String residentRegistrationNumber
) {
}
