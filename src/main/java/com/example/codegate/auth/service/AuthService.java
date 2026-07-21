package com.example.codegate.auth.service;

import com.example.codegate.auth.client.KakaoOAuthClient;
import com.example.codegate.auth.dto.HospitalLoginRequest;
import com.example.codegate.auth.dto.HospitalSignupRequest;
import com.example.codegate.auth.dto.KakaoLoginUrlResponse;
import com.example.codegate.auth.dto.KakaoLoginRequest;
import com.example.codegate.auth.dto.KakaoUserInfo;
import com.example.codegate.auth.dto.LoginResponse;
import com.example.codegate.auth.dto.PatientKakaoSignupRequest;
import com.example.codegate.global.BusinessException;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.hospital.repository.HospitalRepository;
import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.District;
import com.example.codegate.reservation.support.HospitalProfileParser;
import com.example.codegate.user.entity.PatientProfile;
import com.example.codegate.user.entity.UserAccount;
import com.example.codegate.user.entity.UserRole;
import com.example.codegate.user.repository.PatientProfileRepository;
import com.example.codegate.user.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserAccountRepository userAccountRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final CryptoService cryptoService;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final HospitalProfileParser hospitalProfileParser;

    public AuthService(
            KakaoOAuthClient kakaoOAuthClient,
            UserAccountRepository userAccountRepository,
            PatientProfileRepository patientProfileRepository,
            HospitalRepository hospitalRepository,
            CryptoService cryptoService,
            PasswordService passwordService,
            JwtTokenService jwtTokenService,
            HospitalProfileParser hospitalProfileParser
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.userAccountRepository = userAccountRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.cryptoService = cryptoService;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
        this.hospitalProfileParser = hospitalProfileParser;
    }

    public KakaoLoginUrlResponse createKakaoLoginUrl(String redirectUri, String state) {
        return new KakaoLoginUrlResponse(kakaoOAuthClient.createLoginUrl(redirectUri, state));
    }

    @Transactional
    public LoginResponse signupPatientWithKakao(PatientKakaoSignupRequest request) {
        KakaoUserInfo kakaoUserInfo = kakaoOAuthClient.getUserInfoByAuthorizationCode(request.code(), request.redirectUri());

        UserAccount userAccount = userAccountRepository.findByKakaoId(kakaoUserInfo.kakaoId())
                .orElseGet(() -> userAccountRepository.save(UserAccount.kakaoPatient(kakaoUserInfo.kakaoId())));

        if (userAccount.getRole() != UserRole.PATIENT) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_ACCOUNT_ROLE", "일반 사용자 계정으로 가입할 수 없는 계정입니다.");
        }

        patientProfileRepository.findByUserAccount(userAccount)
                .ifPresent(profile -> {
                    throw new BusinessException(HttpStatus.CONFLICT, "PATIENT_ALREADY_SIGNED_UP", "이미 일반 사용자 회원가입이 완료된 계정입니다.");
                });

        String encryptedResidentNumber = cryptoService.encrypt(request.residentRegistrationNumber().replace("-", ""));
        patientProfileRepository.save(new PatientProfile(
                userAccount,
                request.name(),
                request.gender(),
                request.birthDate(),
                encryptedResidentNumber
        ));

        return loginResponse(userAccount);
    }

    @Transactional(readOnly = true)
    public LoginResponse loginPatientWithKakao(KakaoLoginRequest request) {
        KakaoUserInfo kakaoUserInfo = kakaoOAuthClient.getUserInfoByAuthorizationCode(request.code(), request.redirectUri());

        UserAccount userAccount = userAccountRepository.findByKakaoId(kakaoUserInfo.kakaoId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PATIENT_SIGNUP_REQUIRED", "일반 사용자 추가 회원가입이 필요합니다."));

        if (userAccount.getRole() != UserRole.PATIENT) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "일반 사용자 계정이 아닙니다.");
        }

        patientProfileRepository.findByUserAccount(userAccount)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PATIENT_PROFILE_REQUIRED", "일반 사용자 추가 회원가입 정보가 필요합니다."));

        return loginResponse(userAccount);
    }

    @Transactional
    public LoginResponse signupHospital(HospitalSignupRequest request) {
        if (userAccountRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "LOGIN_ID_DUPLICATED", "이미 사용 중인 병원 로그인 ID입니다.");
        }

        UserAccount userAccount = userAccountRepository.save(
                UserAccount.hospitalLocal(request.loginId(), passwordService.hash(request.password()))
        );
        hospitalRepository.save(new Hospital(
                userAccount,
                request.hospitalName(),
                request.hospitalLocation(),
                resolveDistrict(request.district(), request.hospitalLocation()),
                request.availableTime(),
                subjectsText(request.medicalSubjects(), request.departments()),
                resolveDepartments(request.departments(), request.medicalSubjects())
        ));

        return loginResponse(userAccount);
    }

    @Transactional(readOnly = true)
    public LoginResponse loginHospital(HospitalLoginRequest request) {
        UserAccount userAccount = userAccountRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (userAccount.getRole() != UserRole.HOSPITAL || !passwordService.matches(request.password(), userAccount.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return loginResponse(userAccount);
    }

    private LoginResponse loginResponse(UserAccount userAccount) {
        return new LoginResponse(
                jwtTokenService.createToken(userAccount.getId(), userAccount.getRole()),
                "Bearer",
                userAccount.getId(),
                userAccount.getRole()
        );
    }

    private District resolveDistrict(String districtValue, String location) {
        District district = District.fromLabel(districtValue);
        if (district == null) {
            district = hospitalProfileParser.parseDistrictText(location);
        }
        if (district == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DISTRICT_REQUIRED",
                    "병원 지역구(district)는 필수이며 서울시 자치구 코드 또는 이름이어야 합니다.");
        }
        return district;
    }

    private java.util.Set<Department> resolveDepartments(java.util.List<String> departmentValues, String medicalSubjects) {
        java.util.Set<Department> departments = new java.util.LinkedHashSet<>();
        if (departmentValues != null) {
            for (String value : departmentValues) {
                Department department = Department.fromLabel(value);
                if (department == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "UNKNOWN_DEPARTMENT",
                            "존재하지 않는 진료과목입니다: " + value);
                }
                departments.add(department);
            }
        }
        if (departments.isEmpty()) {
            departments.addAll(hospitalProfileParser.parseDepartmentsText(medicalSubjects));
        }
        if (departments.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DEPARTMENT_REQUIRED",
                    "진료과목(departments)은 하나 이상 필요합니다.");
        }
        return departments;
    }

    private String subjectsText(String medicalSubjects, java.util.List<String> departments) {
        if (medicalSubjects != null && !medicalSubjects.isBlank()) {
            return medicalSubjects;
        }
        if (departments == null || departments.isEmpty()) {
            return "";
        }
        return String.join(", ", departments);
    }
}
