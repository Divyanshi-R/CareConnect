package com.careconnect.service;

import com.careconnect.dto.ClinicalNoteRequest;
import com.careconnect.dto.ClinicalNoteResponse;
import com.careconnect.entity.ClinicalNote;
import com.careconnect.entity.Doctor;
import com.careconnect.entity.Encounter;
import com.careconnect.entity.Patient;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.ClinicalNoteRepository;
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
public class ClinicalNoteService {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final EncounterRepository encounterRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientService patientService;

    public ClinicalNoteService(
            ClinicalNoteRepository clinicalNoteRepository,
            EncounterRepository encounterRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            PatientService patientService
    ) {
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.encounterRepository = encounterRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientService = patientService;
    }

    @Transactional(readOnly = true)
    public List<ClinicalNoteResponse> getMyNotes(UserDetails requestingUser) {
        Patient patient = patientService.getOwnPatient(requestingUser);
        List<Long> encounterIds = encounterRepository.findByPatientId(patient.getId()).stream()
                .map(Encounter::getId)
                .toList();
        if (encounterIds.isEmpty()) {
            return List.of();
        }

        return clinicalNoteRepository.findByEncounterIdIn(encounterIds).stream()
                .map(ClinicalNoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClinicalNoteResponse createNote(Long encounterId, ClinicalNoteRequest request, UserDetails requestingUser) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", encounterId));

        verifyRequesterCanAccessEncounter(encounter, requestingUser, true);

        ClinicalNote note = new ClinicalNote();
        note.setEncounterId(encounter.getId());
        note.setDoctorId(encounter.getDoctorId());
        note.setNoteType(request.noteType());
        note.setContent(request.content().trim());

        ClinicalNote saved = clinicalNoteRepository.save(note);
        return ClinicalNoteResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicalNoteResponse> getNotesByEncounter(Long encounterId, UserDetails requestingUser) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", encounterId));

        verifyRequesterCanAccessEncounter(encounter, requestingUser, false);

        return clinicalNoteRepository.findByEncounterId(encounterId).stream()
                .map(ClinicalNoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClinicalNoteResponse updateNote(Long noteId, ClinicalNoteRequest request, UserDetails requestingUser) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("ClinicalNote", "id", noteId));

        Encounter encounter = encounterRepository.findById(note.getEncounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", note.getEncounterId()));

        verifyRequesterCanModifyNote(encounter, note, requestingUser);

        note.setNoteType(request.noteType());
        note.setContent(request.content().trim());

        ClinicalNote updated = clinicalNoteRepository.save(note);
        return ClinicalNoteResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteNote(Long noteId, UserDetails requestingUser) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("ClinicalNote", "id", noteId));

        Encounter encounter = encounterRepository.findById(note.getEncounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", note.getEncounterId()));

        verifyRequesterCanModifyNote(encounter, note, requestingUser);
        clinicalNoteRepository.delete(note);
    }

    private void verifyRequesterCanAccessEncounter(Encounter encounter, UserDetails requestingUser, boolean isCreateOperation) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        User currentUser = userRepository.findByEmail(requestingUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.PATIENT) {
            if (isCreateOperation) {
                throw new AccessDeniedException("Patients cannot create clinical notes");
            }

            Patient patient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", currentUser.getId()));
            if (!patient.getId().equals(encounter.getPatientId())) {
                throw new AccessDeniedException("Patients can only view their own encounter notes");
            }
            return;
        }

        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor actingDoctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            if (!actingDoctor.getId().equals(encounter.getDoctorId())) {
                throw new AccessDeniedException("Doctors can only access notes for their own encounters");
            }
            return;
        }

        throw new AccessDeniedException("Access denied");
    }

    private void verifyRequesterCanModifyNote(Encounter encounter, ClinicalNote note, UserDetails requestingUser) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        User currentUser = userRepository.findByEmail(requestingUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.PATIENT) {
            throw new AccessDeniedException("Patients cannot modify clinical notes");
        }

        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor actingDoctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));

            if (!actingDoctor.getId().equals(encounter.getDoctorId())) {
                throw new AccessDeniedException("Doctors can only modify notes for their own encounters");
            }

            if (!note.getDoctorId().equals(encounter.getDoctorId())) {
                throw new AccessDeniedException("Note does not belong to this encounter");
            }
            return;
        }

        throw new AccessDeniedException("Access denied");
    }
}
