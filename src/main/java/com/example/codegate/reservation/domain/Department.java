package com.example.codegate.reservation.domain;

import java.util.Arrays;
import java.util.List;

/**
 * 진료 과목 드롭다운. 사용자가 선택하지 않으면(null) 예약 가능한 모든 진료과를 조회한다.
 *
 * <p>병원 회원가입 시 입력하는 {@code medicalSubjects} 는 "내과, 영상의학과, 종합검진" 같은
 * 자유 문자열이므로, 표기 흔들림을 흡수하기 위해 {@code aliases} 를 둔다.</p>
 */
public enum Department {

    HEALTH_CHECKUP("종합건강검진", "종합검진", "건강검진", "검진"),
    INTERNAL_MEDICINE("내과"),
    FAMILY_MEDICINE("가정의학과", "가정의학"),
    SURGERY("외과", "일반외과"),
    ORTHOPEDICS("정형외과"),
    NEUROLOGY("신경과", "신경외과"),
    ENT("이비인후과"),
    OPHTHALMOLOGY("안과"),
    DERMATOLOGY("피부과"),
    DENTISTRY("치과"),
    OBGYN("산부인과"),
    PEDIATRICS("소아청소년과", "소아과"),
    UROLOGY("비뇨의학과", "비뇨기과"),
    RADIOLOGY("영상의학과", "영상의학", "방사선과");

    private final String label;
    private final List<String> aliases;

    Department(String label, String... aliases) {
        this.label = label;
        this.aliases = List.of(aliases);
    }

    public String getLabel() {
        return label;
    }

    /**
     * 한글 라벨("내과") / 코드명("INTERNAL_MEDICINE") / 별칭("종합검진") 을 모두 허용한다.
     * 매칭 실패 시 null.
     */
    public static Department fromLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(d -> d.matches(trimmed))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String value) {
        return label.equals(value)
                || name().equalsIgnoreCase(value)
                || aliases.contains(value);
    }
}
