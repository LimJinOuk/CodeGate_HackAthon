package com.example.codegate.user.controller;

import com.example.codegate.auth.dto.AuthenticatedUser;
import com.example.codegate.auth.service.JwtTokenService;
import com.example.codegate.global.ApiResponse;
import com.example.codegate.user.dto.PatientProfileRequest;
import com.example.codegate.user.dto.PatientProfileResponse;
import com.example.codegate.user.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;
    private final JwtTokenService jwtTokenService;

    public PatientController(PatientService patientService, JwtTokenService jwtTokenService) {
        this.patientService = patientService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/me")
    public ApiResponse<PatientProfileResponse> getMyPatient(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AuthenticatedUser authenticatedUser = jwtTokenService.parseAuthorizationHeader(authorizationHeader);
        return ApiResponse.ok(patientService.getMyPatient(authenticatedUser));
    }

    /** 부분 수정. 보내지 않은 필드는 기존 값을 유지하고, 빈 배열은 해당 목록을 비운다. */
    @PostMapping("/me")
    public ApiResponse<PatientProfileResponse> updateMyPatient(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody PatientProfileRequest request
    ) {
        AuthenticatedUser authenticatedUser = jwtTokenService.parseAuthorizationHeader(authorizationHeader);
        return ApiResponse.ok(patientService.updateMyPatient(authenticatedUser, request));
    }
}
