package com.careconnect.dto;

import com.careconnect.entity.OrderPriority;
import com.careconnect.entity.OrderStatus;
import com.careconnect.entity.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicalOrderRequest(
        @NotNull(message = "patientId is required") Long patientId,
        @NotNull(message = "doctorId is required") Long doctorId,
        @NotNull(message = "encounterId is required") Long encounterId,
        @NotNull(message = "orderType is required") OrderType orderType,
        @NotBlank(message = "description is required")
        @Size(max = 4000, message = "description cannot exceed 4000 characters") String description,
        @NotNull(message = "priority is required") OrderPriority priority,
        @NotNull(message = "status is required") OrderStatus status
) {
}
