package com.careconnect.controller;

import com.careconnect.dto.PrescriptionRequest;
import com.careconnect.dto.PrescriptionResponse;
import com.careconnect.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping("/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> create(
            @Valid @RequestBody PrescriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(201).body(prescriptionService.create(request, userDetails));
    }

    @GetMapping("/prescriptions/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<PrescriptionResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(prescriptionService.getById(id, userDetails));
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<PrescriptionResponse>> getByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(prescriptionService.getByPatient(patientId, userDetails));
    }

    @GetMapping("/doctors/{doctorId}/prescriptions")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<PrescriptionResponse>> getByDoctor(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(prescriptionService.getByDoctor(doctorId, userDetails));
    }

    @PutMapping("/prescriptions/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(prescriptionService.update(id, request, userDetails));
    }
}
