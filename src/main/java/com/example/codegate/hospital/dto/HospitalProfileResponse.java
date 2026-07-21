package com.example.codegate.hospital.dto;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Department;

import java.util.Comparator;
import java.util.List;

public record HospitalProfileResponse(
        Long hospitalId,
        String hospitalName,
        String hospitalLocation,
        String district,
        String availableTime,
        String medicalSubjects,
        List<String> departments
) {

    public static HospitalProfileResponse from(Hospital hospital) {
        return new HospitalProfileResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalLocation(),
                hospital.getDistrict() == null ? null : hospital.getDistrict().getLabel(),
                hospital.getAvailableTime(),
                hospital.getMedicalSubjects(),
                hospital.getDepartments().stream()
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .map(Department::getLabel)
                        .toList()
        );
    }
}
