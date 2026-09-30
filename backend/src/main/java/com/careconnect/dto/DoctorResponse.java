package com.careconnect.dto;

import com.careconnect.entity.Doctor;

import java.time.LocalDateTime;

public record DoctorResponse(
        Long id,
        Long userId,
        Long departmentId,
        String firstName,
        String lastName,
        String specialization,
        String licenseNumber,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DoctorResponse fromEntity(Doctor d) {
        if (d == null) return null;
        return new DoctorResponse(d.getId(), d.getUserId(), d.getDepartmentId(), d.getFirstName(), d.getLastName(), d.getSpecialization(), d.getLicenseNumber(), d.getPhone(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
