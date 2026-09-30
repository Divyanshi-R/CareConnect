package com.careconnect.dto;

import com.careconnect.entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String phone,
        String address,
        String emergencyContact,
        String bloodGroup,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PatientResponse fromEntity(Patient patient) {
        if (patient == null) return null;
        return new PatientResponse(
                patient.getId(),
                patient.getUserId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getEmergencyContact(),
                patient.getBloodGroup(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }
}
