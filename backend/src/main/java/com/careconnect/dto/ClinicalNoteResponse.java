package com.careconnect.dto;

import com.careconnect.entity.ClinicalNote;
import com.careconnect.entity.NoteType;

import java.time.LocalDateTime;

public record ClinicalNoteResponse(
        Long id,
        Long encounterId,
        Long doctorId,
        NoteType noteType,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ClinicalNoteResponse fromEntity(ClinicalNote note) {
        if (note == null) {
            return null;
        }

        return new ClinicalNoteResponse(
                note.getId(),
                note.getEncounterId(),
                note.getDoctorId(),
                note.getNoteType(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
