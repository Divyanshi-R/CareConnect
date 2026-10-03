package com.careconnect.dto;

import com.careconnect.entity.PrescriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PrescriptionRequest(
        @NotNull(message = "patientId is required")
        Long patientId,
        @NotNull(message = "doctorId is required")
        Long doctorId,
        @NotNull(message = "encounterId is required")
        Long encounterId,
        @NotNull(message = "prescribedDate is required")
        LocalDate prescribedDate,
        @Size(max = 4000, message = "instructions cannot exceed 4000 characters")
        String instructions,
        @NotNull(message = "status is required")
        PrescriptionStatus status,
        @NotEmpty(message = "at least one prescription item is required")
        List<@Valid PrescriptionItemRequest> items
) {
}
