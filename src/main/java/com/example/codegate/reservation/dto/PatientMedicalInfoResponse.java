package com.example.codegate.reservation.dto;

import com.example.codegate.reservation.domain.Reservation;
import com.example.codegate.user.entity.Gender;
import com.example.codegate.user.entity.PatientProfile;

import java.time.LocalDate;
import java.util.List;

/**
 * 병원이 예약 건의 환자 의료 정보를 확인할 때 쓰는 응답.
 *
 * <p>예약 목록({@link ReservationResponse})에 섞지 않는 이유 - 목록은 한 페이지에 최대 100건이라
 * 건건이 프로필과 컬렉션 2개를 로딩하면 N+1 이 된다. 상세 조회에서만 가져온다.</p>
 */
public record PatientMedicalInfoResponse(
        Long reservationId,
        Long patientId,
        String patientName,
        Gender gender,
        LocalDate birthDate,
        List<String> medications,
        List<String> diseases
) {

    public static PatientMedicalInfoResponse from(Reservation reservation, PatientProfile profile) {
        return new PatientMedicalInfoResponse(
                reservation.getId(),
                reservation.getPatientId(),
                profile == null ? reservation.getPatientName() : profile.getName(),
                profile == null ? null : profile.getGender(),
                profile == null ? null : profile.getBirthDate(),
                profile == null ? List.of() : profile.getMedications(),
                profile == null ? List.of() : profile.getDiseases()
        );
    }
}
