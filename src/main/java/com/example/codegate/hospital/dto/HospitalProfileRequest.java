package com.example.codegate.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HospitalProfileRequest(
        @NotBlank @Size(max = 120) String hospitalName,
        @NotBlank @Size(max = 255) String hospitalLocation,
        String district,
        @NotBlank @Size(max = 255) String availableTime,
        String medicalSubjects,
        List<String> departments
) {
}
