package com.careconnect.dto;

import com.careconnect.entity.DiagnosisStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record DiagnosisRequest(
        @NotNull(message = "patientId is required") Long patientId,
        @NotNull(message = "doctorId is required") Long doctorId,
        @NotBlank(message = "diagnosisName is required")
        @Size(max = 200, message = "diagnosisName cannot exceed 200 characters") String diagnosisName,
        @NotBlank(message = "description is required")
        @Size(max = 4000, message = "description cannot exceed 4000 characters") String description,
        @NotBlank(message = "diagnosisCode is required")
        @Size(max = 100, message = "diagnosisCode cannot exceed 100 characters") String diagnosisCode,
        @NotNull(message = "diagnosedAt is required") LocalDateTime diagnosedAt,
        @NotNull(message = "status is required") DiagnosisStatus status
) {
}
