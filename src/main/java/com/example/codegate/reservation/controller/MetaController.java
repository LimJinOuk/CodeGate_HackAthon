package com.example.codegate.reservation.controller;

import com.example.codegate.global.ApiResponse;
import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.District;
import com.example.codegate.reservation.dto.CodeItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** 드롭다운 구성용 코드 조회 (로그인 불필요) */
@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    /** 병원 위치 드롭다운 - 서울시 25개 자치구 (필수 선택 항목) */
    @GetMapping("/districts")
    public ApiResponse<List<CodeItem>> districts() {
        return ApiResponse.ok(Arrays.stream(District.values())
                .map(d -> new CodeItem(d.name(), d.getLabel()))
                .toList());
    }

    /** 진료과목 드롭다운 목록 (선택 항목 - 미선택 시 전체 조회) */
    @GetMapping("/departments")
    public ApiResponse<List<CodeItem>> departments() {
        return ApiResponse.ok(Arrays.stream(Department.values())
                .map(d -> new CodeItem(d.name(), d.getLabel()))
                .toList());
    }
}
