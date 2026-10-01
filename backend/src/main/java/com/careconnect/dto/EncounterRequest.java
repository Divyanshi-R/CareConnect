package com.careconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EncounterRequest(
        @NotNull(message = "patientId is required") Long patientId,
        @NotNull(message = "doctorId is required") Long doctorId,
        @NotNull(message = "appointmentId is required") Long appointmentId,
        @NotNull(message = "encounterDate is required") LocalDate encounterDate,
        @NotBlank(message = "chiefComplaint is required") @Size(max = 1000, message = "chiefComplaint cannot exceed 1000 characters") String chiefComplaint,
        @NotBlank(message = "diagnosis is required") @Size(max = 2000, message = "diagnosis cannot exceed 2000 characters") String diagnosis,
        @Size(max = 2000, message = "treatmentPlan cannot exceed 2000 characters") String treatmentPlan,
        @Size(max = 4000, message = "notes cannot exceed 4000 characters") String notes,
        @Size(max = 2000, message = "vitals cannot exceed 2000 characters") String vitals
) {}
