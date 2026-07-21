package com.example.codegate.user.service;

import com.example.codegate.auth.dto.AuthenticatedUser;
import com.example.codegate.auth.service.CryptoService;
import com.example.codegate.global.BusinessException;
import com.example.codegate.user.dto.PatientProfileRequest;
import com.example.codegate.user.dto.PatientProfileResponse;
import com.example.codegate.user.entity.PatientProfile;
import com.example.codegate.user.entity.UserAccount;
import com.example.codegate.user.entity.UserRole;
import com.example.codegate.user.repository.PatientProfileRepository;
import com.example.codegate.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PatientService {

    private final UserAccountRepository userAccountRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final CryptoService cryptoService;

    public PatientService(UserAccountRepository userAccountRepository,
                          PatientProfileRepository patientProfileRepository,
                          CryptoService cryptoService) {
        this.userAccountRepository = userAccountRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true)
    public PatientProfileResponse getMyPatient(AuthenticatedUser authenticatedUser) {
        PatientProfile profile = getPatientProfileFor(authenticatedUser);
        return PatientProfileResponse.from(profile);
    }

    @Transactional
    public PatientProfileResponse updateMyPatient(AuthenticatedUser authenticatedUser, PatientProfileRequest request) {
        PatientProfile profile = getPatientProfileFor(authenticatedUser);
        profile.updateProfile(
                request.name(),
                request.gender(),
                request.birthDate(),
                encryptedResidentNumberOrNull(request.residentRegistrationNumber()),
                request.medications(),
                request.diseases()
        );
        return PatientProfileResponse.from(profile);
    }

    /** 주민번호를 보내지 않았으면 null 을 돌려준다. 엔티티가 이를 "기존 값 유지" 로 해석한다. */
    private String encryptedResidentNumberOrNull(String residentRegistrationNumber) {
        if (!StringUtils.hasText(residentRegistrationNumber)) {
            return null;
        }
        return cryptoService.encrypt(residentRegistrationNumber.replace("-", ""));
    }

    private PatientProfile getPatientProfileFor(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.role() != UserRole.PATIENT) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "PATIENT_ROLE_REQUIRED", "일반 사용자만 접근할 수 있습니다.");
        }

        UserAccount userAccount = userAccountRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));

        return patientProfileRepository.findByUserAccount(userAccount)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", "환자 정보를 찾을 수 없습니다."));
    }
}
