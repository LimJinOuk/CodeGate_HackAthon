package com.example.codegate.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HospitalSignupRequest(
        @NotBlank @Size(min = 4, max = 80) String loginId,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 120) String hospitalName,
        @NotBlank @Size(max = 255) String hospitalLocation,
        String district,
        @NotBlank @Size(max = 255) String availableTime,
        String medicalSubjects,
        List<String> departments
) {
}
