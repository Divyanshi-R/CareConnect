package com.careconnect.controller;

import com.careconnect.dto.ClinicalNoteRequest;
import com.careconnect.dto.ClinicalNoteResponse;
import com.careconnect.service.ClinicalNoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ClinicalNoteController {

    private final ClinicalNoteService clinicalNoteService;

    public ClinicalNoteController(ClinicalNoteService clinicalNoteService) {
        this.clinicalNoteService = clinicalNoteService;
    }

    @PostMapping("/encounters/{encounterId}/notes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<ClinicalNoteResponse> createNote(
            @PathVariable Long encounterId,
            @Valid @RequestBody ClinicalNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ClinicalNoteResponse response = clinicalNoteService.createNote(encounterId, request, userDetails);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/encounters/{encounterId}/notes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<List<ClinicalNoteResponse>> getNotesByEncounter(
            @PathVariable Long encounterId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalNoteService.getNotesByEncounter(encounterId, userDetails));
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<ClinicalNoteResponse> updateNote(
            @PathVariable("id") Long noteId,
            @Valid @RequestBody ClinicalNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalNoteService.updateNote(noteId, request, userDetails));
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<Void> deleteNote(
            @PathVariable("id") Long noteId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        clinicalNoteService.deleteNote(noteId, userDetails);
        return ResponseEntity.noContent().build();
    }
}
