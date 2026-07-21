package com.example.codegate.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HospitalProfileRequest(
        @NotBlank @Size(max = 120) String hospitalName,
        @NotBlank @Size(max = 255) String hospitalLocation,
        @NotBlank @Size(max = 255) String availableTime,
        @NotBlank String medicalSubjects
) {
}
