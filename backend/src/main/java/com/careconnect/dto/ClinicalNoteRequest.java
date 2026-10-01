package com.careconnect.dto;

import com.careconnect.entity.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicalNoteRequest(
        @NotNull(message = "noteType is required") NoteType noteType,
        @NotBlank(message = "content is required") @Size(max = 4000, message = "content cannot exceed 4000 characters") String content
) {}
