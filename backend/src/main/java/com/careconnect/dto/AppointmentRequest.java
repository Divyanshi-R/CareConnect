package com.careconnect.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentRequest(
        // patientId should not be trusted for patient role; enforced in service
        @NotNull(message = "patientId is required") Long patientId,
        @NotNull(message = "doctorId is required") Long doctorId,
        @NotNull(message = "appointmentDate is required") @FutureOrPresent(message = "appointmentDate must be present or future") LocalDate appointmentDate,
        @NotNull(message = "appointmentTime is required") LocalTime appointmentTime,
        @Size(max = 1000) String reason,
        @Size(max = 2000) String notes
) {}
