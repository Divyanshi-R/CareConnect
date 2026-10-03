package com.careconnect.service;

import com.careconnect.dto.DiagnosisRequest;
import com.careconnect.dto.DiagnosisResponse;
import com.careconnect.entity.AuditAction;
import com.careconnect.entity.Diagnosis;
import com.careconnect.entity.Doctor;
import com.careconnect.entity.Encounter;
import com.careconnect.entity.Patient;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.DiagnosisRepository;
import com.careconnect.repository.DoctorRepository;
import com.careconnect.repository.EncounterRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public DiagnosisService(
            DiagnosisRepository diagnosisRepository,
            EncounterRepository encounterRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.diagnosisRepository = diagnosisRepository;
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DiagnosisResponse create(Long encounterId, DiagnosisRequest request, UserDetails requestingUser) {
        Encounter encounter = findEncounter(encounterId);
        validateEncounterRelationships(encounter);
        verifyEncounterAccess(encounter, requestingUser, true);
        validateRequestRelationships(encounter, request);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setEncounterId(encounter.getId());
        diagnosis.setPatientId(encounter.getPatientId());
        diagnosis.setDoctorId(encounter.getDoctorId());
        applyRequest(diagnosis, request);

        Diagnosis saved = diagnosisRepository.save(diagnosis);
        auditLogService.record(AuditAction.DIAGNOSIS_CREATED, requestingUser, "Diagnosis", saved.getId());
        return DiagnosisResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getByEncounter(Long encounterId, UserDetails requestingUser) {
        Encounter encounter = findEncounter(encounterId);
        validateEncounterRelationships(encounter);
        verifyEncounterAccess(encounter, requestingUser, false);

        return diagnosisRepository.findByEncounterId(encounterId).stream()
                .map(diagnosis -> validateDiagnosisRelationships(diagnosis, encounter))
                .map(DiagnosisResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getByPatient(Long patientId, UserDetails requestingUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));
        User currentUser = findCurrentUser(requestingUser);

        if (currentUser.getRole() == Role.PATIENT) {
            Patient ownPatient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", currentUser.getId()));
            if (!ownPatient.getId().equals(patient.getId())) {
                throw new AccessDeniedException("Patients can only view their own diagnoses");
            }
        }

        Doctor actingDoctor = null;
        if (currentUser.getRole() == Role.DOCTOR) {
            actingDoctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
        } else if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Access denied");
        }

        final Doctor authorizedDoctor = actingDoctor;
        return diagnosisRepository.findByPatientId(patientId).stream()
                .map(diagnosis -> {
                    Encounter encounter = findEncounter(diagnosis.getEncounterId());
                    return validateDiagnosisRelationships(diagnosis, encounter);
                })
                .filter(diagnosis -> authorizedDoctor == null
                        || authorizedDoctor.getId().equals(diagnosis.getDoctorId()))
                .map(DiagnosisResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DiagnosisResponse update(Long id, DiagnosisRequest request, UserDetails requestingUser) {
        Diagnosis diagnosis = diagnosisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnosis", "id", id));
        Encounter encounter = findEncounter(diagnosis.getEncounterId());
        validateDiagnosisRelationships(diagnosis, encounter);
        verifyEncounterAccess(encounter, requestingUser, false);
        validateRequestRelationships(encounter, request);

        applyRequest(diagnosis, request);
        return DiagnosisResponse.fromEntity(diagnosisRepository.save(diagnosis));
    }

    private Encounter findEncounter(Long encounterId) {
        return encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", encounterId));
    }

    private void validateEncounterRelationships(Encounter encounter) {
        patientRepository.findById(encounter.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", encounter.getPatientId()));
        doctorRepository.findById(encounter.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", encounter.getDoctorId()));
    }

    private Diagnosis validateDiagnosisRelationships(Diagnosis diagnosis, Encounter encounter) {
        validateEncounterRelationships(encounter);
        if (!diagnosis.getEncounterId().equals(encounter.getId())
                || !diagnosis.getPatientId().equals(encounter.getPatientId())
                || !diagnosis.getDoctorId().equals(encounter.getDoctorId())) {
            throw new IllegalArgumentException("Diagnosis patient and doctor must match its encounter");
        }
        return diagnosis;
    }

    private void validateRequestRelationships(Encounter encounter, DiagnosisRequest request) {
        if (!encounter.getPatientId().equals(request.patientId())) {
            throw new IllegalArgumentException("Diagnosis patientId must match the encounter patient");
        }
        if (!encounter.getDoctorId().equals(request.doctorId())) {
            throw new IllegalArgumentException("Diagnosis doctorId must match the encounter doctor");
        }
    }

    private void verifyEncounterAccess(Encounter encounter, UserDetails requestingUser, boolean createOperation) {
        User currentUser = findCurrentUser(requestingUser);
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.PATIENT) {
            if (createOperation) {
                throw new AccessDeniedException("Patients cannot create diagnoses");
            }
            Patient patient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", currentUser.getId()));
            if (!patient.getId().equals(encounter.getPatientId())) {
                throw new AccessDeniedException("Patients can only view their own diagnoses");
            }
            return;
        }

        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            if (!doctor.getId().equals(encounter.getDoctorId())) {
                throw new AccessDeniedException("Doctors can only access diagnoses for their own encounters");
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

    private void applyRequest(Diagnosis diagnosis, DiagnosisRequest request) {
        diagnosis.setDiagnosisName(request.diagnosisName().trim());
        diagnosis.setDescription(request.description().trim());
        diagnosis.setDiagnosisCode(request.diagnosisCode().trim());
        diagnosis.setDiagnosedAt(request.diagnosedAt());
        diagnosis.setStatus(request.status());
    }
}
