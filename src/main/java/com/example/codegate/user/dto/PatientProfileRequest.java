package com.example.codegate.user.dto;

import com.example.codegate.user.entity.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 환자 본인 정보 수정 요청. 모든 필드가 선택이며 <b>보내지 않은 값은 기존 값을 유지</b>한다.
 *
 * <p>Jakarta 검증 애너테이션은 모두 null 을 통과시키므로 "생략 = 유지" 가 별도 분기 없이 성립한다.</p>
 */
public record PatientProfileRequest(
        @Size(max = 50) String name,

        Gender gender,

        @Past LocalDate birthDate,

        @Pattern(regexp = "^\\d{6}-?\\d{7}$", message = "주민등록번호는 13자리 숫자 또는 hyphen 포함 형식이어야 합니다.")
        String residentRegistrationNumber,

        /** 빈 배열을 보내면 복용 중인 약을 전부 비운다. */
        @Size(max = 30) List<@Size(max = 100) String> medications,

        /** 빈 배열을 보내면 앓고 있는 질병을 전부 비운다. */
        @Size(max = 30) List<@Size(max = 100) String> diseases
) {
}
