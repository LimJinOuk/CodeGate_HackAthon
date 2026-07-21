package com.example.codegate.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Entity
@Table(name = "patient_profiles")
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 512)
    private String residentRegistrationNumberEncrypted;

    /**
     * 현재 복용 중인 약. 회원가입 화면의 다중 선택 버튼 라벨과 "기타" 자유 입력을
     * 구분 없이 같은 리스트에 담는다.
     *
     * <p>별도 테이블이 아니라 patient_profiles 의 JSON 칼럼에 저장한다.
     * 환자 한 명의 정보가 한 행에 모여 있어야 DB 에서 바로 확인할 수 있기 때문이다.
     * 조회 시 join 이 없어 N+1 걱정도 없다.</p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medications", columnDefinition = "json")
    private List<String> medications = new ArrayList<>();

    /** 앓고 있는 질병. 저장 방식은 {@link #medications} 와 동일하다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diseases", columnDefinition = "json")
    private List<String> diseases = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected PatientProfile() {
    }

    public PatientProfile(
            UserAccount userAccount,
            String name,
            Gender gender,
            LocalDate birthDate,
            String residentRegistrationNumberEncrypted
    ) {
        this(userAccount, name, gender, birthDate, residentRegistrationNumberEncrypted, null, null);
    }

    public PatientProfile(
            UserAccount userAccount,
            String name,
            Gender gender,
            LocalDate birthDate,
            String residentRegistrationNumberEncrypted,
            List<String> medications,
            List<String> diseases
    ) {
        this.userAccount = userAccount;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.residentRegistrationNumberEncrypted = residentRegistrationNumberEncrypted;
        this.medications = normalize(medications);
        this.diseases = normalize(diseases);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 부분 수정. null 인 리스트, null 이거나 공백인 문자열 인자는 기존 값을 유지한다.
     * 빈 리스트를 넘기면 해당 항목을 전부 비운다.
     *
     * <p>주민등록번호는 이미 암호화된 문자열을 받는다. 평문이 엔티티 밖으로 나가지
     * 않도록 복호화 getter 를 두지 않기 때문이다.</p>
     */
    public void updateProfile(String name,
                              Gender gender,
                              LocalDate birthDate,
                              String residentRegistrationNumberEncrypted,
                              List<String> medications,
                              List<String> diseases) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (StringUtils.hasText(residentRegistrationNumberEncrypted)) {
            this.residentRegistrationNumberEncrypted = residentRegistrationNumberEncrypted;
        }
        if (medications != null) {
            this.medications = normalize(medications);
        }
        if (diseases != null) {
            this.diseases = normalize(diseases);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /** 앞뒤 공백 제거 후 빈 값과 중복을 걸러낸다. 프론트가 보낸 순서는 그대로 유지한다. */
    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                normalized.add(value.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public List<String> getMedications() {
        if (medications == null) {
            return List.of();
        }
        return List.copyOf(medications);
    }

    public List<String> getDiseases() {
        if (diseases == null) {
            return List.of();
        }
        return List.copyOf(diseases);
    }
}
