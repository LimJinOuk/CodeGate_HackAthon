package com.example.codegate.hospital.dto;

import com.example.codegate.hospital.entity.Hospital;

public record HospitalProfileResponse(
        Long hospitalId,
        String hospitalName,
        String hospitalLocation,
        String availableTime,
        String medicalSubjects
) {

    public static HospitalProfileResponse from(Hospital hospital) {
        return new HospitalProfileResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalLocation(),
                hospital.getAvailableTime(),
                hospital.getMedicalSubjects()
        );
    }
}
