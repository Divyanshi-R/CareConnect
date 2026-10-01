package com.careconnect.dto;

import com.careconnect.entity.Encounter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EncounterResponse(
        Long id,
        Long patientId,
        Long doctorId,
        Long appointmentId,
        LocalDate encounterDate,
        String chiefComplaint,
        String diagnosis,
        String treatmentPlan,
        String notes,
        String vitals,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EncounterResponse fromEntity(Encounter encounter) {
        if (encounter == null) {
            return null;
        }

        return new EncounterResponse(
                encounter.getId(),
                encounter.getPatientId(),
                encounter.getDoctorId(),
                encounter.getAppointmentId(),
                encounter.getEncounterDate(),
                encounter.getChiefComplaint(),
                encounter.getDiagnosis(),
                encounter.getTreatmentPlan(),
                encounter.getNotes(),
                encounter.getVitals(),
                encounter.getCreatedAt(),
                encounter.getUpdatedAt()
        );
    }
}
