package com.example.codegate.auth.controller;

import com.example.codegate.auth.dto.HospitalLoginRequest;
import com.example.codegate.auth.dto.HospitalSignupRequest;
import com.example.codegate.auth.dto.KakaoLoginRequest;
import com.example.codegate.auth.dto.KakaoLoginUrlResponse;
import com.example.codegate.auth.dto.LoginResponse;
import com.example.codegate.auth.dto.PatientKakaoSignupRequest;
import com.example.codegate.auth.service.AuthService;
import com.example.codegate.global.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/kakao/login-url")
    public ApiResponse<KakaoLoginUrlResponse> kakaoLoginUrl(
            @RequestParam @NotBlank String redirectUri,
            @RequestParam(required = false) String state
    ) {
        return ApiResponse.ok(authService.createKakaoLoginUrl(redirectUri, state));
    }

    @PostMapping("/patients/kakao/signup")
    public ApiResponse<LoginResponse> patientKakaoSignup(@Valid @RequestBody PatientKakaoSignupRequest request) {
        return ApiResponse.ok(authService.signupPatientWithKakao(request));
    }

    @PostMapping("/patients/kakao/login")
    public ApiResponse<LoginResponse> patientKakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ApiResponse.ok(authService.loginPatientWithKakao(request));
    }

    @PostMapping("/hospitals/signup")
    public ApiResponse<LoginResponse> hospitalSignup(@Valid @RequestBody HospitalSignupRequest request) {
        return ApiResponse.ok(authService.signupHospital(request));
    }

    @PostMapping("/hospitals/login")
    public ApiResponse<LoginResponse> hospitalLogin(@Valid @RequestBody HospitalLoginRequest request) {
        return ApiResponse.ok(authService.loginHospital(request));
    }
}
