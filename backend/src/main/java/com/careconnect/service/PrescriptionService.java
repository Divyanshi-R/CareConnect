package com.careconnect.service;

import com.careconnect.dto.PrescriptionItemRequest;
import com.careconnect.dto.PrescriptionItemResponse;
import com.careconnect.dto.PrescriptionRequest;
import com.careconnect.dto.PrescriptionResponse;
import com.careconnect.entity.Doctor;
import com.careconnect.entity.Encounter;
import com.careconnect.entity.Medication;
import com.careconnect.entity.Patient;
import com.careconnect.entity.Prescription;
import com.careconnect.entity.PrescriptionItem;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.DoctorRepository;
import com.careconnect.repository.EncounterRepository;
import com.careconnect.repository.MedicationRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.PrescriptionItemRepository;
import com.careconnect.repository.PrescriptionRepository;
import com.careconnect.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;

    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            PrescriptionItemRepository prescriptionItemRepository,
            EncounterRepository encounterRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            MedicationRepository medicationRepository,
            UserRepository userRepository
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.medicationRepository = medicationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PrescriptionResponse create(PrescriptionRequest request, UserDetails requestingUser) {
        Encounter encounter = findEncounter(request.encounterId());
        Patient patient = findPatient(request.patientId());
        Doctor doctor = findDoctor(request.doctorId());
        validateRequestRelationships(patient, doctor, encounter);
        verifyDoctorForEncounter(encounter, requestingUser);
        validateMedications(request.items());

        Prescription prescription = new Prescription();
        prescription.setPatientId(encounter.getPatientId());
        prescription.setDoctorId(encounter.getDoctorId());
        prescription.setEncounterId(encounter.getId());
        applyRequest(prescription, request);
        Prescription saved = prescriptionRepository.save(prescription);

        List<PrescriptionItem> items = request.items().stream()
                .map(item -> toEntity(saved.getId(), item))
                .toList();
        prescriptionItemRepository.saveAll(items);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getById(Long id, UserDetails requestingUser) {
        Prescription prescription = findPrescription(id);
        validatePrescriptionRelationships(prescription);
        verifyPrescriptionAccess(prescription, requestingUser);
        return toResponse(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getByPatient(Long patientId, UserDetails requestingUser) {
        Patient patient = findPatient(patientId);
        User currentUser = findCurrentUser(requestingUser);
        if (currentUser.getRole() == Role.PATIENT) {
            Patient ownPatient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            if (!ownPatient.getId().equals(patient.getId())) {
                throw new AccessDeniedException("Patients can only view their own prescriptions");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }

        return prescriptionRepository.findByPatientId(patientId).stream()
                .map(prescription -> {
                    validatePrescriptionRelationships(prescription);
                    return toResponse(prescription);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getByDoctor(Long doctorId, UserDetails requestingUser) {
        Doctor doctor = findDoctor(doctorId);
        User currentUser = findCurrentUser(requestingUser);
        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor ownDoctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            if (!ownDoctor.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only view their own prescriptions");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }

        return prescriptionRepository.findByDoctorId(doctorId).stream()
                .map(prescription -> {
                    validatePrescriptionRelationships(prescription);
                    return toResponse(prescription);
                })
                .toList();
    }

    @Transactional
    public PrescriptionResponse update(Long id, PrescriptionRequest request, UserDetails requestingUser) {
        Prescription prescription = findPrescription(id);
        Encounter encounter = validatePrescriptionRelationships(prescription);
        verifyDoctorForEncounter(encounter, requestingUser);

        if (!prescription.getPatientId().equals(request.patientId())
                || !prescription.getDoctorId().equals(request.doctorId())
                || !prescription.getEncounterId().equals(request.encounterId())) {
            throw new IllegalArgumentException("Cannot change prescription patient, doctor, or encounter");
        }
        validateMedications(request.items());

        applyRequest(prescription, request);
        Prescription saved = prescriptionRepository.save(prescription);
        prescriptionItemRepository.deleteByPrescriptionId(id);
        prescriptionItemRepository.saveAll(request.items().stream()
                .map(item -> toEntity(id, item))
                .toList());
        return toResponse(saved);
    }

    private Prescription findPrescription(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
    }

    private Encounter findEncounter(Long id) {
        return encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", id));
    }

    private Patient findPatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }

    private Doctor findDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    private void validateRequestRelationships(Patient patient, Doctor doctor, Encounter encounter) {
        if (!patient.getId().equals(encounter.getPatientId())) {
            throw new IllegalArgumentException("Prescription patientId must match the encounter patient");
        }
        if (!doctor.getId().equals(encounter.getDoctorId())) {
            throw new IllegalArgumentException("Prescription doctorId must match the encounter doctor");
        }
    }

    private Encounter validatePrescriptionRelationships(Prescription prescription) {
        Encounter encounter = findEncounter(prescription.getEncounterId());
        findPatient(prescription.getPatientId());
        findDoctor(prescription.getDoctorId());
        if (!prescription.getPatientId().equals(encounter.getPatientId())
                || !prescription.getDoctorId().equals(encounter.getDoctorId())) {
            throw new IllegalArgumentException("Prescription patient and doctor must match its encounter");
        }
        return encounter;
    }

    private void verifyDoctorForEncounter(Encounter encounter, UserDetails requestingUser) {
        User currentUser = findCurrentUser(requestingUser);
        if (currentUser.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors can create or update prescriptions");
        }

        Doctor actingDoctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (!actingDoctor.getId().equals(encounter.getDoctorId())) {
            throw new AccessDeniedException("Doctors can only manage prescriptions for their own encounters");
        }
    }

    private void verifyPrescriptionAccess(Prescription prescription, UserDetails requestingUser) {
        User currentUser = findCurrentUser(requestingUser);
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            if (!patient.getId().equals(prescription.getPatientId())) {
                throw new AccessDeniedException("Patients can only view their own prescriptions");
            }
            return;
        }
        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            if (!doctor.getId().equals(prescription.getDoctorId())) {
                throw new AccessDeniedException("Doctors can only view their own prescriptions");
            }
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private User findCurrentUser(UserDetails requestingUser) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByEmail(requestingUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));
    }

    private void validateMedications(List<PrescriptionItemRequest> items) {
        for (PrescriptionItemRequest item : items) {
            if (!medicationRepository.existsById(item.medicationId())) {
                throw new ResourceNotFoundException("Medication", "id", item.medicationId());
            }
        }
    }

    private void applyRequest(Prescription prescription, PrescriptionRequest request) {
        prescription.setPrescribedDate(request.prescribedDate());
        prescription.setInstructions(request.instructions());
        prescription.setStatus(request.status());
    }

    private PrescriptionItem toEntity(Long prescriptionId, PrescriptionItemRequest request) {
        PrescriptionItem item = new PrescriptionItem();
        item.setPrescriptionId(prescriptionId);
        item.setMedicationId(request.medicationId());
        item.setDosage(request.dosage());
        item.setFrequency(request.frequency());
        item.setDuration(request.duration());
        item.setRoute(request.route());
        item.setInstructions(request.instructions());
        return item;
    }

    private PrescriptionResponse toResponse(Prescription prescription) {
        List<PrescriptionItemResponse> items = prescriptionItemRepository
                .findByPrescriptionId(prescription.getId())
                .stream()
                .map(PrescriptionItemResponse::fromEntity)
                .toList();
        return PrescriptionResponse.fromEntity(prescription, items);
    }
}
