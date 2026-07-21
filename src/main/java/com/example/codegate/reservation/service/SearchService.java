package com.example.codegate.reservation.service;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.hospital.repository.HospitalRepository;
import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.District;
import com.example.codegate.reservation.domain.ScheduleSlot;
import com.example.codegate.reservation.dto.HospitalSearchResult;
import com.example.codegate.reservation.dto.SearchResponse;
import com.example.codegate.reservation.dto.SlotResponse;
import com.example.codegate.reservation.repository.ScheduleSlotRepository;
import com.example.codegate.reservation.support.HospitalProfileParser;
import com.example.codegate.reservation.support.ReservationErrors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자의 예약 조회.
 *
 * <p>필터 우선순위 - 지역구(필수) → 진료과목(선택) → 날짜(선택) → 시간대(선택).
 * 진료과목을 선택하지 않으면 해당 조건에서 예약 가능한 모든 진료과를 함께 내려준다.</p>
 *
 * <p>지역구는 병원 주소 문자열에서 해석하므로, 병원 필터링만 애플리케이션에서 수행하고
 * 시간대 조회는 DB 쿼리로 처리한다.</p>
 */
@Service
public class SearchService {

    private final HospitalRepository hospitalRepository;
    private final ScheduleSlotRepository slotRepository;
    private final HospitalProfileParser profileParser;

    public SearchService(HospitalRepository hospitalRepository,
                         ScheduleSlotRepository slotRepository,
                         HospitalProfileParser profileParser) {
        this.hospitalRepository = hospitalRepository;
        this.slotRepository = slotRepository;
        this.profileParser = profileParser;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String districtParam, String departmentParam,
                                 LocalDate date, LocalTime fromTime, LocalTime toTime) {

        District district = requireDistrict(districtParam);
        Department department = parseDepartmentOrNull(departmentParam);

        if (fromTime != null && toTime != null && !toTime.isAfter(fromTime)) {
            throw ReservationErrors.invalidTimeRange();
        }

        // 1) 주소에서 지역구를 해석해 대상 병원을 추린다
        Map<Long, Hospital> hospitalById = new LinkedHashMap<>();
        for (Hospital hospital : hospitalRepository.findAll()) {
            if (profileParser.parseDistrict(hospital) == district) {
                hospitalById.put(hospital.getId(), hospital);
            }
        }
        if (hospitalById.isEmpty()) {
            return emptyResponse(district, department, date, fromTime, toTime);
        }

        // 2) 지역구 + 날짜 + 시간 조건만 적용한 "예약 가능 슬롯"
        LocalDateTime now = LocalDateTime.now();
        List<ScheduleSlot> slotsBeforeDepartmentFilter = slotRepository.findAvailableSlots(
                        hospitalById.keySet(), now.toLocalDate(), now.toLocalTime(), date, fromTime, toTime)
                .stream()
                .filter(s -> toTime == null || !s.getEndTime().isAfter(toTime))
                .toList();

        // 진료과 드롭다운을 선택하지 않았을 때 노출할 "가능 진료과" 전체 목록
        List<String> allAvailableDepartments = distinctDepartmentLabels(slotsBeforeDepartmentFilter);

        // 3) 진료과목 필터 적용
        List<ScheduleSlot> matched = slotsBeforeDepartmentFilter.stream()
                .filter(s -> department == null || s.getDepartment() == department)
                .toList();

        // 4) 병원 단위로 묶기
        Map<Long, List<ScheduleSlot>> grouped = new LinkedHashMap<>();
        for (ScheduleSlot slot : matched) {
            grouped.computeIfAbsent(slot.getHospitalId(), k -> new ArrayList<>()).add(slot);
        }

        List<HospitalSearchResult> results = new ArrayList<>();
        for (Map.Entry<Long, List<ScheduleSlot>> entry : grouped.entrySet()) {
            Hospital hospital = hospitalById.get(entry.getKey());
            List<ScheduleSlot> slots = entry.getValue();

            results.add(new HospitalSearchResult(
                    hospital.getId(),
                    hospital.getHospitalName(),
                    district.getLabel(),
                    hospital.getHospitalLocation(),
                    hospital.getAvailableTime(),
                    profileParser.parseDepartments(hospital).stream().map(Department::getLabel).toList(),
                    distinctDepartmentLabels(slots),
                    slots.size(),
                    slots.stream().map(s -> SlotResponse.from(s, now)).toList()
            ));
        }
        results.sort(Comparator.comparing(HospitalSearchResult::hospitalId));

        return new SearchResponse(
                district.getLabel(),
                department == null ? null : department.getLabel(),
                date == null ? null : date.toString(),
                formatTimeRange(fromTime, toTime),
                "Asia/Seoul",
                allAvailableDepartments,
                results.size(),
                matched.size(),
                results
        );
    }

    // ------------------------------------------------------------------ 내부용

    private District requireDistrict(String value) {
        if (value == null || value.isBlank()) {
            throw ReservationErrors.districtRequired();
        }
        District district = District.fromLabel(value);
        if (district == null) {
            throw ReservationErrors.unknownDistrict(value);
        }
        return district;
    }

    private Department parseDepartmentOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Department department = Department.fromLabel(value);
        if (department == null) {
            throw ReservationErrors.unknownDepartment(value);
        }
        return department;
    }

    private List<String> distinctDepartmentLabels(List<ScheduleSlot> slots) {
        return slots.stream()
                .map(ScheduleSlot::getDepartment)
                .distinct()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(Department::getLabel)
                .toList();
    }

    private SearchResponse emptyResponse(District district, Department department,
                                         LocalDate date, LocalTime fromTime, LocalTime toTime) {
        return new SearchResponse(
                district.getLabel(),
                department == null ? null : department.getLabel(),
                date == null ? null : date.toString(),
                formatTimeRange(fromTime, toTime),
                "Asia/Seoul",
                List.of(), 0, 0, List.of());
    }

    private String formatTimeRange(LocalTime from, LocalTime to) {
        if (from == null && to == null) {
            return null;
        }
        return (from == null ? "00:00" : from.toString()) + " ~ " + (to == null ? "23:59" : to.toString());
    }
}
