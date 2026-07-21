package com.example.codegate.reservation.support;

import com.example.codegate.auth.dto.AuthenticatedUser;
import com.example.codegate.auth.service.JwtTokenService;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.hospital.repository.HospitalRepository;
import com.example.codegate.user.entity.UserAccount;
import com.example.codegate.user.entity.UserRole;
import com.example.codegate.user.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/**
 * 예약 API 의 요청자 확인.
 *
 * <p>인증 자체는 인증 모듈의 {@link JwtTokenService} 에 위임하고,
 * 여기서는 역할 검증과 계정/병원 조회만 담당한다.</p>
 */
@Component
public class CallerResolver {

    private final JwtTokenService jwtTokenService;
    private final UserAccountRepository userAccountRepository;
    private final HospitalRepository hospitalRepository;

    public CallerResolver(JwtTokenService jwtTokenService,
                          UserAccountRepository userAccountRepository,
                          HospitalRepository hospitalRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userAccountRepository = userAccountRepository;
        this.hospitalRepository = hospitalRepository;
    }

    /** 사용자(PATIENT) 계정이어야 하는 API */
    public UserAccount requirePatient(String authorizationHeader) {
        AuthenticatedUser authenticated = jwtTokenService.parseAuthorizationHeader(authorizationHeader);
        if (authenticated.role() != UserRole.PATIENT) {
            throw ReservationErrors.patientRoleRequired();
        }
        return userAccountRepository.findById(authenticated.userId())
                .orElseThrow(ReservationErrors::userNotFound);
    }

    /** 병원(HOSPITAL) 계정이어야 하는 API. 계정에 연결된 병원 정보를 돌려준다. */
    public Hospital requireHospital(String authorizationHeader) {
        AuthenticatedUser authenticated = jwtTokenService.parseAuthorizationHeader(authorizationHeader);
        if (authenticated.role() != UserRole.HOSPITAL) {
            throw ReservationErrors.hospitalRoleRequired();
        }
        UserAccount userAccount = userAccountRepository.findById(authenticated.userId())
                .orElseThrow(ReservationErrors::userNotFound);
        return hospitalRepository.findByUserAccount(userAccount)
                .orElseThrow(ReservationErrors::hospitalNotFound);
    }
}
