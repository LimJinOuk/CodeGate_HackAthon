package com.example.codegate.reservation.dto;

import java.util.List;

/**
 * 예약 조회 결과.
 *
 * @param selectedDepartment  사용자가 선택한 진료과목. 선택하지 않았으면 null 이고,
 *                            이때 {@code allAvailableDepartments} 에 해당 지역의 예약 가능한 전체 진료과가 담긴다.
 */
public record SearchResponse(
        String district,
        String selectedDepartment,
        String date,
        String timeRange,
        String timeZone,
        List<String> allAvailableDepartments,
        int hospitalCount,
        int slotCount,
        List<HospitalSearchResult> hospitals
) {
}
