package com.example.codegate.seed;

import com.example.codegate.auth.service.CryptoService;
import com.example.codegate.auth.service.PasswordService;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.hospital.repository.HospitalRepository;
import com.example.codegate.user.entity.Gender;
import com.example.codegate.user.entity.PatientProfile;
import com.example.codegate.user.entity.UserAccount;
import com.example.codegate.user.repository.PatientProfileRepository;
import com.example.codegate.user.repository.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "codegate.seed.mock", name = "enabled", havingValue = "true")
public class MockDataSeeder implements ApplicationRunner {

    private static final String MOCK_HOSPITAL_PASSWORD = "Password123!";

    private final UserAccountRepository userAccountRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordService passwordService;
    private final CryptoService cryptoService;

    public MockDataSeeder(
            UserAccountRepository userAccountRepository,
            PatientProfileRepository patientProfileRepository,
            HospitalRepository hospitalRepository,
            PasswordService passwordService,
            CryptoService cryptoService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordService = passwordService;
        this.cryptoService = cryptoService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPatients();
        seedHospitals();
    }

    private void seedPatients() {
        List<PatientSeed> patients = List.of(
                new PatientSeed("mock-kakao-patient-m-01", "김민준", Gender.MALE, 20, "0607213123456"),
                new PatientSeed("mock-kakao-patient-m-02", "이서준", Gender.MALE, 24, "0207213123457"),
                new PatientSeed("mock-kakao-patient-m-03", "박도윤", Gender.MALE, 29, "9707211123458"),
                new PatientSeed("mock-kakao-patient-m-04", "최지호", Gender.MALE, 35, "9107211123459"),
                new PatientSeed("mock-kakao-patient-m-05", "정현우", Gender.MALE, 42, "8407211123460"),
                new PatientSeed("mock-kakao-patient-m-06", "강준호", Gender.MALE, 48, "7807211123461"),
                new PatientSeed("mock-kakao-patient-m-07", "조민성", Gender.MALE, 55, "7107211123462"),
                new PatientSeed("mock-kakao-patient-m-08", "윤지훈", Gender.MALE, 63, "6307211123463"),
                new PatientSeed("mock-kakao-patient-m-09", "장태민", Gender.MALE, 71, "5507211123464"),
                new PatientSeed("mock-kakao-patient-m-10", "임성민", Gender.MALE, 80, "4607211123465"),
                new PatientSeed("mock-kakao-patient-f-01", "김서연", Gender.FEMALE, 21, "0507214123456"),
                new PatientSeed("mock-kakao-patient-f-02", "이하윤", Gender.FEMALE, 26, "0007214123457"),
                new PatientSeed("mock-kakao-patient-f-03", "박지아", Gender.FEMALE, 31, "9507212123458"),
                new PatientSeed("mock-kakao-patient-f-04", "최수빈", Gender.FEMALE, 38, "8807212123459"),
                new PatientSeed("mock-kakao-patient-f-05", "정유진", Gender.FEMALE, 44, "8207212123460"),
                new PatientSeed("mock-kakao-patient-f-06", "강민서", Gender.FEMALE, 50, "7607212123461"),
                new PatientSeed("mock-kakao-patient-f-07", "조예은", Gender.FEMALE, 57, "6907212123462"),
                new PatientSeed("mock-kakao-patient-f-08", "윤채원", Gender.FEMALE, 65, "6107212123463"),
                new PatientSeed("mock-kakao-patient-f-09", "장수아", Gender.FEMALE, 73, "5307212123464"),
                new PatientSeed("mock-kakao-patient-f-10", "임하은", Gender.FEMALE, 79, "4707212123465")
        );

        for (PatientSeed patient : patients) {
            if (userAccountRepository.findByKakaoId(patient.kakaoId()).isPresent()) {
                continue;
            }

            UserAccount userAccount = userAccountRepository.save(UserAccount.kakaoPatient(patient.kakaoId()));
            patientProfileRepository.save(new PatientProfile(
                    userAccount,
                    patient.name(),
                    patient.gender(),
                    birthDateFromAge(patient.age()),
                    cryptoService.encrypt(patient.residentRegistrationNumber())
            ));
        }
    }

    private void seedHospitals() {
        List<DistrictSeed> districts = List.of(
                new DistrictSeed("gangnam", "강남구"),
                new DistrictSeed("gangdong", "강동구"),
                new DistrictSeed("gangbuk", "강북구"),
                new DistrictSeed("gangseo", "강서구"),
                new DistrictSeed("gwanak", "관악구"),
                new DistrictSeed("gwangjin", "광진구"),
                new DistrictSeed("guro", "구로구"),
                new DistrictSeed("geumcheon", "금천구"),
                new DistrictSeed("nowon", "노원구"),
                new DistrictSeed("dobong", "도봉구"),
                new DistrictSeed("dongdaemun", "동대문구"),
                new DistrictSeed("dongjak", "동작구"),
                new DistrictSeed("mapo", "마포구"),
                new DistrictSeed("seodaemun", "서대문구"),
                new DistrictSeed("seocho", "서초구"),
                new DistrictSeed("seongdong", "성동구"),
                new DistrictSeed("seongbuk", "성북구"),
                new DistrictSeed("songpa", "송파구"),
                new DistrictSeed("yangcheon", "양천구"),
                new DistrictSeed("yeongdeungpo", "영등포구"),
                new DistrictSeed("yongsan", "용산구"),
                new DistrictSeed("eunpyeong", "은평구"),
                new DistrictSeed("jongno", "종로구"),
                new DistrictSeed("jung", "중구"),
                new DistrictSeed("jungnang", "중랑구")
        );

        for (DistrictSeed district : districts) {
            for (int index = 1; index <= 10; index++) {
                String loginId = "hospital_" + district.slug() + "_" + String.format("%02d", index);
                if (userAccountRepository.existsByLoginId(loginId)) {
                    continue;
                }

                UserAccount userAccount = userAccountRepository.save(
                        UserAccount.hospitalLocal(loginId, passwordService.hash(MOCK_HOSPITAL_PASSWORD))
                );
                hospitalRepository.save(new Hospital(
                        userAccount,
                        hospitalName(district.name(), index),
                        "서울특별시 " + district.name() + " " + hospitalRoadName(index) + " " + (10 + index),
                        availableTime(index),
                        medicalSubjects(index)
                ));
            }
        }
    }

    private LocalDate birthDateFromAge(int age) {
        return LocalDate.now().minusYears(age);
    }

    private String hospitalName(String districtName, int index) {
        String[] suffixes = {"메디컬센터", "건강검진센터", "내과의원", "영상의학센터", "종합클리닉"};
        return districtName + " " + suffixes[(index - 1) % suffixes.length] + " " + index + "호점";
    }

    private String hospitalRoadName(int index) {
        String[] roads = {"중앙로", "테헤란로", "대로", "건강로", "의료로"};
        return roads[(index - 1) % roads.length];
    }

    private String availableTime(int index) {
        return switch (index % 5) {
            case 1 -> "월-금 09:00-18:00";
            case 2 -> "월-토 09:00-13:00";
            case 3 -> "월-금 08:30-17:30";
            case 4 -> "월-금 10:00-19:00";
            default -> "월-토 08:00-16:00";
        };
    }

    private String medicalSubjects(int index) {
        return switch (index % 6) {
            case 1 -> "내과, 종합검진, 혈액검사";
            case 2 -> "영상의학과, X-Ray, CT";
            case 3 -> "정형외과, X-Ray, MRI";
            case 4 -> "가정의학과, 종합검진, 예방접종";
            case 5 -> "소화기내과, 위내시경, 대장내시경";
            default -> "내과, 영상의학과, CT, MRI, X-Ray";
        };
    }

    private record PatientSeed(
            String kakaoId,
            String name,
            Gender gender,
            int age,
            String residentRegistrationNumber
    ) {
    }

    private record DistrictSeed(String slug, String name) {
    }
}
