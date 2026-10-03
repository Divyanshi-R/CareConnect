package com.careconnect.controller;

import com.careconnect.dto.DiagnosisRequest;
import com.careconnect.dto.DiagnosisResponse;
import com.careconnect.service.DiagnosisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @PostMapping("/encounters/{encounterId}/diagnoses")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<DiagnosisResponse> createDiagnosis(
            @PathVariable Long encounterId,
            @Valid @RequestBody DiagnosisRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(201).body(diagnosisService.create(encounterId, request, userDetails));
    }

    @GetMapping("/encounters/{encounterId}/diagnoses")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<List<DiagnosisResponse>> getDiagnosesByEncounter(
            @PathVariable Long encounterId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(diagnosisService.getByEncounter(encounterId, userDetails));
    }

    @GetMapping("/patients/{patientId}/diagnoses")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<List<DiagnosisResponse>> getDiagnosesByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(diagnosisService.getByPatient(patientId, userDetails));
    }

    @PutMapping("/diagnoses/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<DiagnosisResponse> updateDiagnosis(
            @PathVariable Long id,
            @Valid @RequestBody DiagnosisRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(diagnosisService.update(id, request, userDetails));
    }
}
