package com.example.codegate.hospital.service;

import com.example.codegate.auth.dto.AuthenticatedUser;
import com.example.codegate.global.BusinessException;
import com.example.codegate.hospital.dto.HospitalProfileRequest;
import com.example.codegate.hospital.dto.HospitalProfileResponse;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.hospital.repository.HospitalRepository;
import com.example.codegate.user.entity.UserAccount;
import com.example.codegate.user.entity.UserRole;
import com.example.codegate.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalService {

    private final UserAccountRepository userAccountRepository;
    private final HospitalRepository hospitalRepository;

    public HospitalService(UserAccountRepository userAccountRepository, HospitalRepository hospitalRepository) {
        this.userAccountRepository = userAccountRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional(readOnly = true)
    public HospitalProfileResponse getMyHospital(AuthenticatedUser authenticatedUser) {
        Hospital hospital = getHospitalFor(authenticatedUser);
        return HospitalProfileResponse.from(hospital);
    }

    @Transactional
    public HospitalProfileResponse updateMyHospital(AuthenticatedUser authenticatedUser, HospitalProfileRequest request) {
        Hospital hospital = getHospitalFor(authenticatedUser);
        hospital.updateProfile(
                request.hospitalName(),
                request.hospitalLocation(),
                request.availableTime(),
                request.medicalSubjects()
        );
        return HospitalProfileResponse.from(hospital);
    }

    private Hospital getHospitalFor(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.role() != UserRole.HOSPITAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HOSPITAL_ROLE_REQUIRED", "병원 회원만 접근할 수 있습니다.");
        }

        UserAccount userAccount = userAccountRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));

        return hospitalRepository.findByUserAccount(userAccount)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", "병원 정보를 찾을 수 없습니다."));
    }
}
