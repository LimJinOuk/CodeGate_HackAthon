package com.example.codegate.hospital.repository;

import com.example.codegate.hospital.entity.Hospital;
import com.example.codegate.reservation.domain.District;
import com.example.codegate.user.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    /**
     * departments 를 함께 가져온다. 이 메서드로 읽은 병원은 트랜잭션 밖(컨트롤러)으로
     * 나가서도 진료과목을 참조하므로 지연 로딩 상태로 두면 안 된다.
     */
    @EntityGraph(attributePaths = "departments")
    Optional<Hospital> findByUserAccount(UserAccount userAccount);

    /**
     * 예약 검색용. 지역구가 채워져 있으면 컬럼으로, 아직 비어 있는 예전 데이터는
     * 주소 문자열로 매칭한다. 주소 매칭은 자치구 이름을 두 개 이상 포함하는 주소까지
     * 걸러오므로, 호출부에서 파서로 한 번 더 확인한다.
     */
    @EntityGraph(attributePaths = "departments")
    @Query("""
            select h from Hospital h
            where h.district = :district
               or (h.district is null and h.hospitalLocation like concat('%', :districtLabel, '%'))
            order by h.id asc
            """)
    List<Hospital> findAllInDistrict(@Param("district") District district,
                                     @Param("districtLabel") String districtLabel);
}
