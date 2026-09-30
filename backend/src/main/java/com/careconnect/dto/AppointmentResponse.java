package com.careconnect.dto;

import com.careconnect.entity.Appointment;
import com.careconnect.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        Long doctorId,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        AppointmentStatus status,
        String reason,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppointmentResponse fromEntity(Appointment a) {
        if (a == null) return null;
        return new AppointmentResponse(
                a.getId(),
                a.getPatientId(),
                a.getDoctorId(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getStatus(),
                a.getReason(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
