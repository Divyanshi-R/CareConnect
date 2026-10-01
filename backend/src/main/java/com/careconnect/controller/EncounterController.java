package com.careconnect.controller;

import com.careconnect.dto.EncounterRequest;
import com.careconnect.dto.EncounterResponse;
import com.careconnect.service.EncounterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterService encounterService;

    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<EncounterResponse> createEncounter(
            @Valid @RequestBody EncounterRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        EncounterResponse response = encounterService.create(request, userDetails);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<EncounterResponse> getEncounter(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(encounterService.getById(id, userDetails));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<List<EncounterResponse>> listByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(encounterService.listByPatient(patientId, userDetails));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<List<EncounterResponse>> listByDoctor(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(encounterService.listByDoctor(doctorId, userDetails));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<EncounterResponse> updateEncounter(
            @PathVariable Long id,
            @Valid @RequestBody EncounterRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(encounterService.update(id, request, userDetails));
    }
}
