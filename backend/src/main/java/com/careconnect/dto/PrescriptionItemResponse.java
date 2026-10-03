package com.careconnect.dto;

import com.careconnect.entity.PrescriptionItem;

public record PrescriptionItemResponse(
        Long id,
        Long prescriptionId,
        Long medicationId,
        String dosage,
        String frequency,
        String duration,
        String route,
        String instructions
) {
    public static PrescriptionItemResponse fromEntity(PrescriptionItem item) {
        if (item == null) {
            return null;
        }

        return new PrescriptionItemResponse(
                item.getId(),
                item.getPrescriptionId(),
                item.getMedicationId(),
                item.getDosage(),
                item.getFrequency(),
                item.getDuration(),
                item.getRoute(),
                item.getInstructions()
        );
    }
}
