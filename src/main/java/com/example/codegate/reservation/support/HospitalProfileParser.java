package com.example.codegate.reservation.support;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.District;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 병원 회원가입 정보(자유 문자열)를 예약 검색에 쓸 수 있는 코드 값으로 해석한다.
 *
 * <p>인증 모듈의 {@code Hospital} 엔티티는 아래처럼 사람이 읽는 문자열만 갖고 있다.</p>
 * <pre>
 *   hospitalLocation : "서울 강남구 삼성동"
 *   medicalSubjects  : "내과, 영상의학과, 종합검진"
 * </pre>
 *
 * <p>예약 검색은 지역구 드롭다운과 진료과 필터가 필요하므로 이 클래스가 문자열을
 * {@link District} / {@link Department} 로 변환한다.
 * 팀원 엔티티를 수정하지 않기 위한 선택이며, 매칭에 실패한 값은 조용히 무시된다.</p>
 */
@Component
public class HospitalProfileParser {

    /** 진료 항목 구분자: 쉼표, 슬래시, 가운뎃점, 줄바꿈 */
    private static final String SUBJECT_DELIMITER = "[,/·\\n\\r]+";

    /**
     * 주소 문자열에서 서울시 자치구를 찾는다.
     *
     * @return 매칭되는 자치구가 없으면 null (해당 병원은 지역구 검색에 잡히지 않는다)
     */
    public District parseDistrict(Hospital hospital) {
        String location = hospital.getHospitalLocation();
        if (location == null || location.isBlank()) {
            return null;
        }
        return Arrays.stream(District.values())
                .filter(d -> location.contains(d.getLabel()))
                .findFirst()
                .orElse(null);
    }

    /**
     * "내과, 영상의학과, 종합검진" → [내과, 영상의학과, 종합건강검진]
     *
     * <p>{@link Department} 의 별칭까지 대조하며, 알 수 없는 항목은 건너뛴다.</p>
     */
    public Set<Department> parseDepartments(Hospital hospital) {
        Set<Department> result = new LinkedHashSet<>();
        String subjects = hospital.getMedicalSubjects();
        if (subjects == null || subjects.isBlank()) {
            return result;
        }
        for (String token : subjects.split(SUBJECT_DELIMITER)) {
            Department department = Department.fromLabel(token.trim());
            if (department != null) {
                result.add(department);
            }
        }
        return result;
    }

    /** 병원이 해당 진료과를 등록해 두었는지 */
    public boolean supports(Hospital hospital, Department department) {
        return parseDepartments(hospital).contains(department);
    }
}
