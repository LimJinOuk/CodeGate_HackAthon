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
import org.springframework.util.StringUtils;

import java.util.Map;

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
    private final PendingPatientSignupStore pendingPatientSignupStore;

    public AuthService(
            KakaoOAuthClient kakaoOAuthClient,
            UserAccountRepository userAccountRepository,
            PatientProfileRepository patientProfileRepository,
            HospitalRepository hospitalRepository,
            CryptoService cryptoService,
            PasswordService passwordService,
            JwtTokenService jwtTokenService,
            HospitalProfileParser hospitalProfileParser,
            PendingPatientSignupStore pendingPatientSignupStore
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.userAccountRepository = userAccountRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.cryptoService = cryptoService;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
        this.hospitalProfileParser = hospitalProfileParser;
        this.pendingPatientSignupStore = pendingPatientSignupStore;
    }

    public KakaoLoginUrlResponse createKakaoLoginUrl(String redirectUri, String state) {
        return new KakaoLoginUrlResponse(kakaoOAuthClient.createLoginUrl(redirectUri, state));
    }

    @Transactional
    public LoginResponse signupPatientWithKakao(PatientKakaoSignupRequest request) {
        UserAccount userAccount = resolvePatientSignupUserAccount(request);

        if (userAccount.getRole() != UserRole.PATIENT) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_ACCOUNT_ROLE", "일반 사용자 계정으로 가입할 수 없는 계정입니다.");
        }

        patientProfileRepository.findByUserAccount(userAccount)
                .ifPresent(profile -> {
                    throw new BusinessException(HttpStatus.CONFLICT, "PATIENT_ALREADY_SIGNED_UP", "이미 일반 사용자 회원가입이 완료된 계정입니다.");
                });

        String encryptedResidentNumber = cryptoService.encrypt(request.residentRegistrationNumber().replace("-", ""));
        PatientProfile patientProfile = patientProfileRepository.save(new PatientProfile(
                userAccount,
                request.name(),
                request.gender(),
                request.birthDate(),
                encryptedResidentNumber
        ));

        return loginResponse(userAccount, patientProfile.getName());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse loginPatientWithKakao(KakaoLoginRequest request) {
        KakaoUserInfo kakaoUserInfo = kakaoOAuthClient.getUserInfoByAuthorizationCode(request.code(), request.redirectUri());

        UserAccount userAccount = userAccountRepository.findByKakaoId(kakaoUserInfo.kakaoId())
                .orElseGet(() -> userAccountRepository.save(UserAccount.kakaoPatient(kakaoUserInfo.kakaoId())));

        if (userAccount.getRole() != UserRole.PATIENT) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "일반 사용자 계정이 아닙니다.");
        }

        PatientProfile patientProfile = patientProfileRepository.findByUserAccount(userAccount)
                .orElseThrow(() -> signupRequired(userAccount));

        return loginResponse(userAccount, patientProfile.getName());
    }

    private UserAccount resolvePatientSignupUserAccount(PatientKakaoSignupRequest request) {
        if (StringUtils.hasText(request.signupToken())) {
            Long userAccountId = pendingPatientSignupStore.consume(request.signupToken())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            "PATIENT_SIGNUP_TOKEN_EXPIRED",
                            "추가 회원가입 인증 정보가 만료되었습니다. 카카오 로그인을 다시 진행해 주세요."
                    ));
            return userAccountRepository.findById(userAccountId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            "PATIENT_SIGNUP_TOKEN_INVALID",
                            "추가 회원가입 인증 정보가 올바르지 않습니다."
                    ));
        }

        if (!StringUtils.hasText(request.code()) || !StringUtils.hasText(request.redirectUri())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "KAKAO_SIGNUP_AUTH_REQUIRED",
                    "카카오 인가 코드 또는 추가 회원가입 토큰이 필요합니다."
            );
        }

        KakaoUserInfo kakaoUserInfo = kakaoOAuthClient.getUserInfoByAuthorizationCode(request.code(), request.redirectUri());
        return userAccountRepository.findByKakaoId(kakaoUserInfo.kakaoId())
                .orElseGet(() -> userAccountRepository.save(UserAccount.kakaoPatient(kakaoUserInfo.kakaoId())));
    }

    private BusinessException signupRequired(UserAccount userAccount) {
        String signupToken = pendingPatientSignupStore.create(userAccount.getId());
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "PATIENT_SIGNUP_REQUIRED",
                "일반 사용자 추가 회원가입이 필요합니다.",
                Map.of("signupToken", signupToken)
        );
    }

    @Transactional
    public LoginResponse signupHospital(HospitalSignupRequest request) {
        if (userAccountRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "LOGIN_ID_DUPLICATED", "이미 사용 중인 병원 로그인 ID입니다.");
        }

        UserAccount userAccount = userAccountRepository.save(
                UserAccount.hospitalLocal(request.loginId(), passwordService.hash(request.password()))
        );
        Hospital hospital = hospitalRepository.save(new Hospital(
                userAccount,
                request.hospitalName(),
                request.hospitalLocation(),
                resolveDistrict(request.district(), request.hospitalLocation()),
                request.availableTime(),
                subjectsText(request.medicalSubjects(), request.departments()),
                resolveDepartments(request.departments(), request.medicalSubjects())
        ));

        return loginResponse(userAccount, hospital.getHospitalName());
    }

    @Transactional(readOnly = true)
    public LoginResponse loginHospital(HospitalLoginRequest request) {
        UserAccount userAccount = userAccountRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (userAccount.getRole() != UserRole.HOSPITAL || !passwordService.matches(request.password(), userAccount.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        Hospital hospital = hospitalRepository.findByUserAccount(userAccount)
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "HOSPITAL_PROFILE_NOT_FOUND", "병원 프로필을 찾을 수 없습니다."));

        return loginResponse(userAccount, hospital.getHospitalName());
    }

    private LoginResponse loginResponse(UserAccount userAccount, String userName) {
        return new LoginResponse(
                jwtTokenService.createToken(userAccount.getId(), userAccount.getRole()),
                "Bearer",
                userAccount.getId(),
                userAccount.getRole(),
                userName
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
