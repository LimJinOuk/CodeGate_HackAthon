package com.example.codegate.hospital.entity;

import com.example.codegate.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

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

    @Column(nullable = false, length = 255)
    private String availableTime;

    @Lob
    @Column(nullable = false)
    private String medicalSubjects;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Hospital() {
    }

    public Hospital(UserAccount userAccount, String hospitalName, String hospitalLocation, String availableTime, String medicalSubjects) {
        this.userAccount = userAccount;
        this.hospitalName = hospitalName;
        this.hospitalLocation = hospitalLocation;
        this.availableTime = availableTime;
        this.medicalSubjects = medicalSubjects;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void updateProfile(String hospitalName, String hospitalLocation, String availableTime, String medicalSubjects) {
        this.hospitalName = hospitalName;
        this.hospitalLocation = hospitalLocation;
        this.availableTime = availableTime;
        this.medicalSubjects = medicalSubjects;
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

    public String getAvailableTime() {
        return availableTime;
    }

    public String getMedicalSubjects() {
        return medicalSubjects;
    }
}
