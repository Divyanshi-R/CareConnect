package com.careconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "prescription_items",
        indexes = {
                @Index(name = "idx_prescription_item_prescription", columnList = "prescription_id"),
                @Index(name = "idx_prescription_item_medication", columnList = "medication_id")
        }
)
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "medication_id", nullable = false)
    private Long medicationId;

    @Column(name = "dosage", nullable = false, length = 200)
    private String dosage;

    @Column(name = "frequency", nullable = false, length = 200)
    private String frequency;

    @Column(name = "duration", nullable = false, length = 200)
    private String duration;

    @Column(name = "route", nullable = false, length = 100)
    private String route;

    @Column(name = "instructions", length = 2000)
    private String instructions;

    public PrescriptionItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
