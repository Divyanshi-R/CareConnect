package com.careconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoctorRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotNull(message = "departmentId is required") Long departmentId,
        @NotBlank(message = "firstName is required") @Size(max = 100) String firstName,
        @NotBlank(message = "lastName is required") @Size(max = 100) String lastName,
        @Size(max = 200) String specialization,
        @NotBlank(message = "licenseNumber is required") @Size(max = 100) String licenseNumber,
        @Size(max = 30) String phone
) {}
