package com.careconnect.service;

import com.careconnect.dto.MedicationRequest;
import com.careconnect.dto.MedicationResponse;
import com.careconnect.entity.Medication;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.MedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicationService {

    private final MedicationRepository medicationRepository;

    public MedicationService(MedicationRepository medicationRepository) {
        this.medicationRepository = medicationRepository;
    }

    @Transactional(readOnly = true)
    public List<MedicationResponse> getAll(String search) {
        List<Medication> medications;
        if (search == null || search.isBlank()) {
            medications = medicationRepository.findAll();
        } else {
            String query = search.trim();
            medications = medicationRepository.findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(query, query);
        }
        return medications.stream().map(MedicationResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public MedicationResponse getById(Long id) {
        return MedicationResponse.fromEntity(findById(id));
    }

    @Transactional
    public MedicationResponse create(MedicationRequest request) {
        Medication medication = new Medication();
        applyRequest(medication, request, true);
        return MedicationResponse.fromEntity(medicationRepository.save(medication));
    }

    @Transactional
    public MedicationResponse update(Long id, MedicationRequest request) {
        Medication medication = findById(id);
        applyRequest(medication, request, false);
        return MedicationResponse.fromEntity(medicationRepository.save(medication));
    }

    @Transactional
    public void delete(Long id) {
        medicationRepository.delete(findById(id));
    }

    private Medication findById(Long id) {
        return medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", "id", id));
    }

    private void applyRequest(Medication medication, MedicationRequest request, boolean creating) {
        medication.setName(request.name());
        medication.setGenericName(request.genericName());
        medication.setForm(request.form());
        medication.setStrength(request.strength());
        medication.setManufacturer(request.manufacturer());
        medication.setDescription(request.description());
        if (request.active() != null) {
            medication.setActive(request.active());
        } else if (creating) {
            medication.setActive(true);
        }
    }
}
