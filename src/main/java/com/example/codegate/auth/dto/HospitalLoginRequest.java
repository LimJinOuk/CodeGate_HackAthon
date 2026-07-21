package com.example.codegate.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record HospitalLoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}
