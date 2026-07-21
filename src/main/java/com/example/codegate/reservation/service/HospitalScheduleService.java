package com.example.codegate.reservation.service;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.ScheduleSlot;
import com.example.codegate.reservation.dto.SlotCreateRequest;
import com.example.codegate.reservation.dto.SlotResponse;
import com.example.codegate.reservation.repository.ReservationRepository;
import com.example.codegate.reservation.repository.ScheduleSlotRepository;
import com.example.codegate.reservation.support.HospitalProfileParser;
import com.example.codegate.reservation.support.ReservationErrors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 병원이 진료 가능한 날짜/시간을 등록·관리한다.
 * 진료는 1시간 단위이며 한 시간에 1명만 받는다.
 */
@Service
public class HospitalScheduleService {

    private final ScheduleSlotRepository slotRepository;
    private final ReservationRepository reservationRepository;
    private final HospitalProfileParser profileParser;

    public HospitalScheduleService(ScheduleSlotRepository slotRepository,
                                   ReservationRepository reservationRepository,
                                   HospitalProfileParser profileParser) {
        this.slotRepository = slotRepository;
        this.reservationRepository = reservationRepository;
        this.profileParser = profileParser;
    }

    /** 우리 병원이 등록한 시간대 목록 (날짜 / 진료과 필터 선택) */
    @Transactional(readOnly = true)
    public List<SlotResponse> findSlots(Hospital hospital, LocalDate date, String departmentParam) {
        Department department = parseDepartmentOrNull(departmentParam);
        LocalDateTime now = LocalDateTime.now();
        return slotRepository.findByHospital_IdOrderBySlotDateAscStartTimeAsc(hospital.getId()).stream()
                .filter(s -> date == null || s.getSlotDate().equals(date))
                .filter(s -> department == null || s.getDepartment() == department)
                .map(s -> SlotResponse.from(s, now))
                .toList();
    }

    /** 우리 병원이 등록한 시간대 목록 (페이지네이션) */
    @Transactional(readOnly = true)
    public Page<SlotResponse> findSlots(Hospital hospital, LocalDate date, String departmentParam, Pageable pageable) {
        Department department = parseDepartmentOrNull(departmentParam);
        LocalDateTime now = LocalDateTime.now();
        Page<ScheduleSlot> slots;
        if (date != null && department != null) {
            slots = slotRepository.findByHospital_IdAndSlotDateAndDepartment(
                    hospital.getId(), date, department, pageable);
        } else if (date != null) {
            slots = slotRepository.findByHospital_IdAndSlotDate(hospital.getId(), date, pageable);
        } else if (department != null) {
            slots = slotRepository.findByHospital_IdAndDepartment(hospital.getId(), department, pageable);
        } else {
            slots = slotRepository.findByHospital_Id(hospital.getId(), pageable);
        }
        return slots.map(s -> SlotResponse.from(s, now));
    }

    /** 시간대 신규 등록. 시작 시각은 정시여야 하며 종료 시각은 +1시간으로 자동 설정된다. */
    @Transactional
    public SlotResponse addSlot(Hospital hospital, SlotCreateRequest request) {
        Department department = Department.fromLabel(request.department());
        if (department == null) {
            throw ReservationErrors.unknownDepartment(request.department());
        }
        if (!profileParser.supports(hospital, department)) {
            throw ReservationErrors.departmentNotSupported(hospital.getHospitalName(), department.getLabel());
        }
        if (request.startTime().getMinute() != 0 || request.startTime().getSecond() != 0) {
            throw ReservationErrors.slotNotOnTheHour(request.startTime());
        }
        if (LocalDateTime.of(request.date(), request.startTime()).isBefore(LocalDateTime.now())) {
            throw ReservationErrors.slotPast();
        }
        if (slotRepository.existsByHospital_IdAndSlotDateAndStartTime(
                hospital.getId(), request.date(), request.startTime())) {
            throw ReservationErrors.slotDuplicated(
                    request.date() + " " + request.startTime());
        }

        ScheduleSlot slot = slotRepository.save(
                new ScheduleSlot(hospital, department, request.date(), request.startTime()));
        return SlotResponse.from(slot, LocalDateTime.now());
    }

    /** 시간대 일괄 등록. 하나라도 유효하지 않으면 전체 트랜잭션을 롤백한다. */
    @Transactional
    public List<SlotResponse> addSlots(Hospital hospital, List<SlotCreateRequest> requests) {
        return requests.stream()
                .map(request -> addSlot(hospital, request))
                .toList();
    }

    /** 시간대 삭제. 예약 이력이 있으면 거부한다. */
    @Transactional
    public void deleteSlot(Hospital hospital, Long slotId) {
        ScheduleSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> ReservationErrors.slotNotFound(slotId));
        if (!slot.getHospitalId().equals(hospital.getId())) {
            throw ReservationErrors.notOwnSlot();
        }
        if (reservationRepository.existsBySlot_Id(slotId)) {
            throw ReservationErrors.slotHasReservation();
        }
        slotRepository.delete(slot);
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
}
