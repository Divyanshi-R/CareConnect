package com.careconnect.dto;

import com.careconnect.entity.Medication;
import com.careconnect.entity.MedicationForm;

import java.time.LocalDateTime;

public record MedicationResponse(
        Long id,
        String name,
        String genericName,
        MedicationForm form,
        String strength,
        String manufacturer,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MedicationResponse fromEntity(Medication medication) {
        if (medication == null) {
            return null;
        }

        return new MedicationResponse(
                medication.getId(),
                medication.getName(),
                medication.getGenericName(),
                medication.getForm(),
                medication.getStrength(),
                medication.getManufacturer(),
                medication.getDescription(),
                medication.isActive(),
                medication.getCreatedAt(),
                medication.getUpdatedAt()
        );
    }
}
