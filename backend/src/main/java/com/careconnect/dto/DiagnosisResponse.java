package com.careconnect.dto;

import com.careconnect.entity.Diagnosis;
import com.careconnect.entity.DiagnosisStatus;

import java.time.LocalDateTime;

public record DiagnosisResponse(
        Long id,
        Long encounterId,
        Long patientId,
        Long doctorId,
        String diagnosisName,
        String description,
        String diagnosisCode,
        LocalDateTime diagnosedAt,
        DiagnosisStatus status
) {
    public static DiagnosisResponse fromEntity(Diagnosis diagnosis) {
        if (diagnosis == null) {
            return null;
        }

        return new DiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getEncounterId(),
                diagnosis.getPatientId(),
                diagnosis.getDoctorId(),
                diagnosis.getDiagnosisName(),
                diagnosis.getDescription(),
                diagnosis.getDiagnosisCode(),
                diagnosis.getDiagnosedAt(),
                diagnosis.getStatus()
        );
    }
}
