package com.careconnect.dto;

import com.careconnect.entity.MedicationForm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MedicationRequest(
        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name cannot exceed 200 characters")
        String name,
        @Size(max = 200, message = "genericName cannot exceed 200 characters")
        String genericName,
        @NotNull(message = "form is required")
        MedicationForm form,
        @NotBlank(message = "strength is required")
        @Size(max = 100, message = "strength cannot exceed 100 characters")
        String strength,
        @Size(max = 200, message = "manufacturer cannot exceed 200 characters")
        String manufacturer,
        @Size(max = 4000, message = "description cannot exceed 4000 characters")
        String description,
        Boolean active
) {
}
