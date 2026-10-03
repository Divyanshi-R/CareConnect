package com.careconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequest(
        @NotNull(message = "medicationId is required")
        Long medicationId,
        @NotBlank(message = "dosage is required")
        @Size(max = 200, message = "dosage cannot exceed 200 characters")
        String dosage,
        @NotBlank(message = "frequency is required")
        @Size(max = 200, message = "frequency cannot exceed 200 characters")
        String frequency,
        @NotBlank(message = "duration is required")
        @Size(max = 200, message = "duration cannot exceed 200 characters")
        String duration,
        @NotBlank(message = "route is required")
        @Size(max = 100, message = "route cannot exceed 100 characters")
        String route,
        @Size(max = 2000, message = "instructions cannot exceed 2000 characters")
        String instructions
) {
}
