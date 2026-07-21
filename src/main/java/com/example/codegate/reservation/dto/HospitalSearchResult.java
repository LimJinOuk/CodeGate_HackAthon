package com.example.codegate.reservation.dto;

import java.util.List;

/**
 * 검색 결과의 병원 1건.
 *
 * @param location              병원 회원가입 시 입력한 주소 원문
 * @param district              주소에서 해석한 지역구
 * @param availableTime         병원이 등록한 진료 가능 시간 안내 문구
 * @param registeredDepartments 병원이 등록한 진료 항목 중 해석에 성공한 진료과
 * @param availableDepartments  이번 검색 조건에서 실제 예약 가능한 진료과
 *                              (사용자가 진료과를 선택하지 않은 경우 이 목록이 "가능 진료과" 안내가 된다)
 */
public record HospitalSearchResult(
        Long hospitalId,
        String hospitalName,
        String district,
        String location,
        String availableTime,
        List<String> registeredDepartments,
        List<String> availableDepartments,
        int availableSlotCount,
        List<SlotResponse> availableSlots
) {
}
