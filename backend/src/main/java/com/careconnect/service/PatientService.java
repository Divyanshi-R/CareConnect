package com.careconnect.service;

import com.careconnect.dto.PatientRequest;
import com.careconnect.dto.PatientResponse;
import com.careconnect.entity.Patient;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public PatientService(PatientRepository patientRepository, UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listAll() {
        return patientRepository.findAll().stream().map(PatientResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientResponse getById(Long id, UserDetails requestingUser) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));

        // Ownership check for patients role
        if (requestingUser != null) {
            User user = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
            if (user.getRole() == Role.PATIENT && !user.getId().equals(patient.getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
        }

        return PatientResponse.fromEntity(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getMyProfile(UserDetails requestingUser) {
        return PatientResponse.fromEntity(getOwnPatient(requestingUser));
    }

    @Transactional(readOnly = true)
    public Patient getOwnPatient(UserDetails requestingUser) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        User user = userRepository.findByEmail(requestingUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));
        if (user.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Access denied");
        }

        return patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", user.getId()));
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        // verify user exists and is PATIENT role
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));

        if (user.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException("Patient profile must be associated with a user having PATIENT role");
        }

        if (patientRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("A patient profile for this user already exists");
        }

        Patient patient = new Patient();
        patient.setUserId(request.userId());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setAddress(request.address());
        patient.setEmergencyContact(request.emergencyContact());
        patient.setBloodGroup(request.bloodGroup());

        Patient saved = patientRepository.save(patient);
        return PatientResponse.fromEntity(saved);
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request, UserDetails requestingUser) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));

        User user = userRepository.findById(patient.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", patient.getUserId()));

        // Patients cannot change ownership
        if (!patient.getUserId().equals(request.userId())) {
            throw new IllegalArgumentException("Cannot change patient ownership/userId");
        }

        // Ownership check
        if (requestingUser != null) {
            User reqUser = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
            if (reqUser.getRole() == Role.PATIENT && !reqUser.getId().equals(patient.getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
        }

        // Update allowed fields
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setAddress(request.address());
        patient.setEmergencyContact(request.emergencyContact());
        patient.setBloodGroup(request.bloodGroup());

        Patient saved = patientRepository.save(patient);
        return PatientResponse.fromEntity(saved);
    }
}
