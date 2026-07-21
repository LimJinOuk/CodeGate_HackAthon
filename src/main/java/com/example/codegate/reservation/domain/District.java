package com.example.codegate.reservation.domain;

import java.util.Arrays;

/**
 * 병원 위치 드롭다운에 사용되는 지역구(서울시 자치구).
 * API 에서는 한글 라벨("강남구")로 주고받는다.
 */
public enum District {

    GANGNAM("강남구"),
    GANGDONG("강동구"),
    GANGBUK("강북구"),
    GANGSEO("강서구"),
    GWANAK("관악구"),
    GWANGJIN("광진구"),
    GURO("구로구"),
    GEUMCHEON("금천구"),
    NOWON("노원구"),
    DOBONG("도봉구"),
    DONGDAEMUN("동대문구"),
    DONGJAK("동작구"),
    MAPO("마포구"),
    SEODAEMUN("서대문구"),
    SEOCHO("서초구"),
    SEONGDONG("성동구"),
    SEONGBUK("성북구"),
    SONGPA("송파구"),
    YANGCHEON("양천구"),
    YEONGDEUNGPO("영등포구"),
    YONGSAN("용산구"),
    EUNPYEONG("은평구"),
    JONGNO("종로구"),
    JUNG("중구"),
    JUNGNANG("중랑구");

    private final String label;

    District(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** "강남구" 또는 "GANGNAM" 모두 허용. 매칭 실패 시 null. */
    public static District fromLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(d -> d.label.equals(trimmed) || d.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}
