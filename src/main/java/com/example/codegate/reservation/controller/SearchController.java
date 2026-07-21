package com.example.codegate.reservation.controller;

import com.example.codegate.global.ApiResponse;
import com.example.codegate.reservation.dto.SearchResponse;
import com.example.codegate.reservation.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 예약 조회 (로그인 없이도 가능).
 *
 * <pre>
 * GET /api/v1/hospitals/search?district=강남구           (필수)
 *                            &amp;department=내과            (선택 - 미선택 시 전체 진료과)
 *                            &amp;date=2026-07-22            (선택 - 캘린더에서 고른 날짜)
 *                            &amp;fromTime=09:00&amp;toTime=12:00 (선택 - 희망 시간 범위)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/hospitals")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ApiResponse<SearchResponse> search(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime toTime
    ) {
        return ApiResponse.ok(searchService.search(district, department, date, fromTime, toTime));
    }
}
