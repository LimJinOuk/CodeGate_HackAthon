package com.example.codegate.hospital.service;

import com.example.codegate.auth.dto.AuthenticatedUser;
import com.example.codegate.global.BusinessException;
import com.example.codegate.hospital.dto.HospitalProfileRequest;
import com.example.codegate.hospital.dto.HospitalProfileResponse;
import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.hospital.repository.HospitalRepository;
import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.District;
import com.example.codegate.reservation.support.HospitalProfileParser;
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
    private final HospitalProfileParser hospitalProfileParser;

    public HospitalService(UserAccountRepository userAccountRepository,
                           HospitalRepository hospitalRepository,
                           HospitalProfileParser hospitalProfileParser) {
        this.userAccountRepository = userAccountRepository;
        this.hospitalRepository = hospitalRepository;
        this.hospitalProfileParser = hospitalProfileParser;
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
                resolveDistrict(request.district(), request.hospitalLocation()),
                request.availableTime(),
                subjectsText(request.medicalSubjects(), request.departments()),
                resolveDepartments(request.departments(), request.medicalSubjects())
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
