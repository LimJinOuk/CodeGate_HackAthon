package com.example.codegate.user.dto;

import com.example.codegate.user.entity.Gender;
import com.example.codegate.user.entity.PatientProfile;

import java.time.LocalDate;
import java.util.List;

/**
 * 환자 본인 정보 응답.
 *
 * <p>주민등록번호는 담지 않는다. 암호화 저장된 값이라 되돌려 줄 수단이 없고,
 * 되돌려 줄 이유도 없다.</p>
 */
public record PatientProfileResponse(
        Long patientId,
        String name,
        Gender gender,
        LocalDate birthDate,
        List<String> medications,
        List<String> diseases
) {

    public static PatientProfileResponse from(PatientProfile profile) {
        return new PatientProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getGender(),
                profile.getBirthDate(),
                profile.getMedications(),
                profile.getDiseases()
        );
    }
}
