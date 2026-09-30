package com.careconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PatientRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotBlank(message = "firstName is required") @Size(max = 100) String firstName,
        @NotBlank(message = "lastName is required") @Size(max = 100) String lastName,
        LocalDate dateOfBirth,
        @Size(max = 20) String gender,
        @Size(max = 30) String phone,
        @Size(max = 500) String address,
        @Size(max = 200) String emergencyContact,
        @Size(max = 20) String bloodGroup
) {
}
