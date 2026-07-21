package com.example.codegate.hospital.entity;

import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.District;
import com.example.codegate.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "hospitals")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(nullable = false, length = 120)
    private String hospitalName;

    @Column(nullable = false, length = 255)
    private String hospitalLocation;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private District district;

    @Column(nullable = false, length = 255)
    private String availableTime;

    @Lob
    @Column(nullable = false)
    private String medicalSubjects;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_departments", joinColumns = @JoinColumn(name = "hospital_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false, length = 40)
    private Set<Department> departments = new LinkedHashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Hospital() {
    }

    public Hospital(UserAccount userAccount, String hospitalName, String hospitalLocation, String availableTime, String medicalSubjects) {
        this(userAccount, hospitalName, hospitalLocation, null, availableTime, medicalSubjects, Set.of());
    }

    public Hospital(UserAccount userAccount, String hospitalName, String hospitalLocation, District district,
                    String availableTime, String medicalSubjects, Set<Department> departments) {
        this.userAccount = userAccount;
        this.hospitalName = hospitalName;
        this.hospitalLocation = hospitalLocation;
        this.district = district;
        this.availableTime = availableTime;
        this.medicalSubjects = medicalSubjects;
        this.departments = departments == null ? new LinkedHashSet<>() : new LinkedHashSet<>(departments);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void updateProfile(String hospitalName, String hospitalLocation, String availableTime, String medicalSubjects) {
        updateProfile(hospitalName, hospitalLocation, this.district, availableTime, medicalSubjects, this.departments);
    }

    public void updateProfile(String hospitalName, String hospitalLocation, District district,
                              String availableTime, String medicalSubjects, Set<Department> departments) {
        this.hospitalName = hospitalName;
        this.hospitalLocation = hospitalLocation;
        this.district = district;
        this.availableTime = availableTime;
        this.medicalSubjects = medicalSubjects;
        this.departments = departments == null ? new LinkedHashSet<>() : new LinkedHashSet<>(departments);
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public String getHospitalLocation() {
        return hospitalLocation;
    }

    public District getDistrict() {
        return district;
    }

    public String getAvailableTime() {
        return availableTime;
    }

    public String getMedicalSubjects() {
        return medicalSubjects;
    }

    public Set<Department> getDepartments() {
        if (departments == null) {
            return Set.of();
        }
        return Set.copyOf(departments);
    }
}
