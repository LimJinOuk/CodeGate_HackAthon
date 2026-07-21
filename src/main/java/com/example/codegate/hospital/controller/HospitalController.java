package com.example.codegate.hospital.controller;

import com.example.codegate.auth.dto.AuthenticatedUser;
import com.example.codegate.auth.service.JwtTokenService;
import com.example.codegate.global.ApiResponse;
import com.example.codegate.hospital.dto.HospitalProfileRequest;
import com.example.codegate.hospital.dto.HospitalProfileResponse;
import com.example.codegate.hospital.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hospital")
public class HospitalController {

    private final HospitalService hospitalService;
    private final JwtTokenService jwtTokenService;

    public HospitalController(HospitalService hospitalService, JwtTokenService jwtTokenService) {
        this.hospitalService = hospitalService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/me")
    public ApiResponse<HospitalProfileResponse> getMyHospital(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AuthenticatedUser authenticatedUser = jwtTokenService.parseAuthorizationHeader(authorizationHeader);
        return ApiResponse.ok(hospitalService.getMyHospital(authenticatedUser));
    }

    @PutMapping("/me")
    public ApiResponse<HospitalProfileResponse> updateMyHospital(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody HospitalProfileRequest request
    ) {
        AuthenticatedUser authenticatedUser = jwtTokenService.parseAuthorizationHeader(authorizationHeader);
        return ApiResponse.ok(hospitalService.updateMyHospital(authenticatedUser, request));
    }
}
