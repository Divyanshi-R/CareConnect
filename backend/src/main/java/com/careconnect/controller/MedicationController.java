package com.careconnect.controller;

import com.careconnect.dto.MedicationRequest;
import com.careconnect.dto.MedicationResponse;
import com.careconnect.service.MedicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<List<MedicationResponse>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(medicationService.getAll(search));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<MedicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(medicationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicationResponse> create(@Valid @RequestBody MedicationRequest request) {
        return ResponseEntity.status(201).body(medicationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MedicationRequest request
    ) {
        return ResponseEntity.ok(medicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        medicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
