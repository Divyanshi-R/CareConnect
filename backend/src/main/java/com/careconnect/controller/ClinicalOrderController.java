package com.careconnect.controller;

import com.careconnect.dto.ClinicalOrderRequest;
import com.careconnect.dto.ClinicalOrderResponse;
import com.careconnect.service.ClinicalOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ClinicalOrderController {

    private final ClinicalOrderService clinicalOrderService;

    public ClinicalOrderController(ClinicalOrderService clinicalOrderService) {
        this.clinicalOrderService = clinicalOrderService;
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ClinicalOrderResponse> create(
            @Valid @RequestBody ClinicalOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(201).body(clinicalOrderService.create(request, userDetails));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ClinicalOrderResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalOrderService.getById(id, userDetails));
    }

    @GetMapping("/patients/{patientId}/orders")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<ClinicalOrderResponse>> getByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalOrderService.getByPatient(patientId, userDetails));
    }

    @GetMapping("/doctors/{doctorId}/orders")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<ClinicalOrderResponse>> getByDoctor(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalOrderService.getByDoctor(doctorId, userDetails));
    }

    @PutMapping("/orders/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ClinicalOrderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClinicalOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalOrderService.update(id, request, userDetails));
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ClinicalOrderResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(clinicalOrderService.cancel(id, userDetails));
    }
}
