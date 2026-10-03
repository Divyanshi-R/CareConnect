package com.careconnect.dto;

import com.careconnect.entity.Prescription;
import com.careconnect.entity.PrescriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponse(
        Long id,
        Long patientId,
        Long doctorId,
        Long encounterId,
        LocalDate prescribedDate,
        String instructions,
        PrescriptionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PrescriptionItemResponse> items
) {
    public static PrescriptionResponse fromEntity(
            Prescription prescription,
            List<PrescriptionItemResponse> items
    ) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPatientId(),
                prescription.getDoctorId(),
                prescription.getEncounterId(),
                prescription.getPrescribedDate(),
                prescription.getInstructions(),
                prescription.getStatus(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt(),
                List.copyOf(items)
        );
    }
}
