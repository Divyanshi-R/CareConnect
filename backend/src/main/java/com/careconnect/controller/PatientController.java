package com.careconnect.controller;

import com.careconnect.dto.PatientRequest;
import com.careconnect.dto.PatientResponse;
import com.careconnect.dto.ClinicalNoteResponse;
import com.careconnect.dto.MedicationResponse;
import com.careconnect.service.ClinicalNoteService;
import com.careconnect.service.PatientService;
import com.careconnect.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final PrescriptionService prescriptionService;
    private final ClinicalNoteService clinicalNoteService;

    public PatientController(
            PatientService patientService,
            PrescriptionService prescriptionService,
            ClinicalNoteService clinicalNoteService
    ) {
        this.patientService = patientService;
        this.prescriptionService = prescriptionService;
        this.clinicalNoteService = clinicalNoteService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(patientService.getMyProfile(userDetails));
    }

    @GetMapping("/me/medications")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicationResponse>> getMyMedications(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(prescriptionService.getMyActiveMedications(userDetails));
    }

    @GetMapping("/me/notes")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<ClinicalNoteResponse>> getMyNotes(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalNoteService.getMyNotes(userDetails));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<List<PatientResponse>> listPatients() {
        return ResponseEntity.ok(patientService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(patientService.getById(id, userDetails));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody PatientRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PatientResponse response = patientService.create(request, userDetails);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PatientResponse response = patientService.update(id, request, userDetails);
        return ResponseEntity.ok(response);
    }
}
