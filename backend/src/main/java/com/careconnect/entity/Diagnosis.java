package com.careconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "diagnoses",
        indexes = {
                @Index(name = "idx_diagnosis_encounter", columnList = "encounter_id"),
                @Index(name = "idx_diagnosis_patient", columnList = "patient_id"),
                @Index(name = "idx_diagnosis_doctor", columnList = "doctor_id")
        }
)
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "diagnosis_name", nullable = false, length = 200)
    private String diagnosisName;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "diagnosis_code", nullable = false, length = 100)
    private String diagnosisCode;

    @Column(name = "diagnosed_at", nullable = false)
    private LocalDateTime diagnosedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DiagnosisStatus status;

    public Diagnosis() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public void setDiagnosisName(String diagnosisName) {
        this.diagnosisName = diagnosisName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiagnosisCode() {
        return diagnosisCode;
    }

    public void setDiagnosisCode(String diagnosisCode) {
        this.diagnosisCode = diagnosisCode;
    }

    public LocalDateTime getDiagnosedAt() {
        return diagnosedAt;
    }

    public void setDiagnosedAt(LocalDateTime diagnosedAt) {
        this.diagnosedAt = diagnosedAt;
    }

    public DiagnosisStatus getStatus() {
        return status;
    }

    public void setStatus(DiagnosisStatus status) {
        this.status = status;
    }
}
