package com.careconnect.service;

import com.careconnect.dto.ClinicalOrderRequest;
import com.careconnect.dto.ClinicalOrderResponse;
import com.careconnect.entity.ClinicalOrder;
import com.careconnect.entity.Doctor;
import com.careconnect.entity.Encounter;
import com.careconnect.entity.OrderStatus;
import com.careconnect.entity.Patient;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.ClinicalOrderRepository;
import com.careconnect.repository.DoctorRepository;
import com.careconnect.repository.EncounterRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClinicalOrderService {

    private final ClinicalOrderRepository clinicalOrderRepository;
    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public ClinicalOrderService(
            ClinicalOrderRepository clinicalOrderRepository,
            EncounterRepository encounterRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            UserRepository userRepository
    ) {
        this.clinicalOrderRepository = clinicalOrderRepository;
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ClinicalOrderResponse create(ClinicalOrderRequest request, UserDetails requestingUser) {
        User currentUser = findCurrentUser(requestingUser);
        Doctor actingDoctor = findDoctorByUserId(currentUser.getId());
        Encounter encounter = findEncounter(request.encounterId());
        Patient patient = findPatient(request.patientId());
        Doctor requestedDoctor = findDoctor(request.doctorId());
        validateRequestRelationships(patient, requestedDoctor, encounter);
        verifyDoctorOwnsEncounter(actingDoctor, encounter);

        ClinicalOrder order = new ClinicalOrder();
        order.setPatientId(encounter.getPatientId());
        order.setDoctorId(encounter.getDoctorId());
        order.setEncounterId(encounter.getId());
        order.setOrderType(request.orderType());
        order.setDescription(request.description().trim());
        order.setPriority(request.priority());
        order.setStatus(OrderStatus.ORDERED);
        order.setOrderedAt(LocalDateTime.now());
        order.setCompletedAt(null);
        return ClinicalOrderResponse.fromEntity(clinicalOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public ClinicalOrderResponse getById(Long id, UserDetails requestingUser) {
        ClinicalOrder order = findOrder(id);
        Encounter encounter = validateOrderRelationships(order);
        verifyReadAccess(order, encounter, requestingUser);
        return ClinicalOrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<ClinicalOrderResponse> getByPatient(Long patientId, UserDetails requestingUser) {
        Patient patient = findPatient(patientId);
        User currentUser = findCurrentUser(requestingUser);

        if (currentUser.getRole() == Role.PATIENT) {
            Patient ownPatient = findPatientByUserId(currentUser.getId());
            if (!ownPatient.getId().equals(patient.getId())) {
                throw new AccessDeniedException("Patients can only view their own orders");
            }
        }

        Doctor actingDoctor = null;
        if (currentUser.getRole() == Role.DOCTOR) {
            actingDoctor = findDoctorByUserId(currentUser.getId());
        } else if (currentUser.getRole() != Role.PATIENT && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }

        Doctor authorizedDoctor = actingDoctor;
        return clinicalOrderRepository.findByPatientId(patientId).stream()
                .map(order -> {
                    Encounter encounter = validateOrderRelationships(order);
                    return new OrderAndEncounter(order, encounter);
                })
                .filter(item -> authorizedDoctor == null
                        || authorizedDoctor.getId().equals(item.encounter().getDoctorId()))
                .map(item -> ClinicalOrderResponse.fromEntity(item.order()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalOrderResponse> getByDoctor(Long doctorId, UserDetails requestingUser) {
        Doctor doctor = findDoctor(doctorId);
        User currentUser = findCurrentUser(requestingUser);

        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor ownDoctor = findDoctorByUserId(currentUser.getId());
            if (!ownDoctor.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only view their own orders");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }

        return clinicalOrderRepository.findByDoctorId(doctorId).stream()
                .map(order -> {
                    validateOrderRelationships(order);
                    return ClinicalOrderResponse.fromEntity(order);
                })
                .toList();
    }

    @Transactional
    public ClinicalOrderResponse update(Long id, ClinicalOrderRequest request, UserDetails requestingUser) {
        ClinicalOrder order = findOrder(id);
        Encounter encounter = validateOrderRelationships(order);
        verifyDoctorOwnsEncounter(findActingDoctor(requestingUser), encounter);

        if (!order.getPatientId().equals(request.patientId())
                || !order.getDoctorId().equals(request.doctorId())
                || !order.getEncounterId().equals(request.encounterId())) {
            throw new IllegalArgumentException("Cannot change clinical order patient, doctor, or encounter");
        }

        validateStatusTransition(order.getStatus(), request.status());
        order.setOrderType(request.orderType());
        order.setDescription(request.description().trim());
        order.setPriority(request.priority());
        updateStatus(order, request.status());
        return ClinicalOrderResponse.fromEntity(clinicalOrderRepository.save(order));
    }

    @Transactional
    public ClinicalOrderResponse cancel(Long id, UserDetails requestingUser) {
        ClinicalOrder order = findOrder(id);
        Encounter encounter = validateOrderRelationships(order);
        verifyDoctorOwnsEncounter(findActingDoctor(requestingUser), encounter);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed orders cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCompletedAt(null);
        return ClinicalOrderResponse.fromEntity(clinicalOrderRepository.save(order));
    }

    private ClinicalOrder findOrder(Long id) {
        return clinicalOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinical order", "id", id));
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

    private Patient findPatientByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
    }

    private Doctor findDoctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
    }

    private User findCurrentUser(UserDetails requestingUser) {
        if (requestingUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByEmail(requestingUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));
    }

    private Doctor findActingDoctor(UserDetails requestingUser) {
        User currentUser = findCurrentUser(requestingUser);
        if (currentUser.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors can manage clinical orders");
        }
        return findDoctorByUserId(currentUser.getId());
    }

    private void validateRequestRelationships(Patient patient, Doctor doctor, Encounter encounter) {
        if (!encounter.getPatientId().equals(patient.getId())) {
            throw new IllegalArgumentException("Clinical order patientId must match the encounter patient");
        }
        if (!encounter.getDoctorId().equals(doctor.getId())) {
            throw new IllegalArgumentException("Clinical order doctorId must match the encounter doctor");
        }
    }

    private Encounter validateOrderRelationships(ClinicalOrder order) {
        Encounter encounter = findEncounter(order.getEncounterId());
        findPatient(order.getPatientId());
        findDoctor(order.getDoctorId());
        if (!order.getPatientId().equals(encounter.getPatientId())
                || !order.getDoctorId().equals(encounter.getDoctorId())) {
            throw new IllegalArgumentException("Clinical order patient and doctor must match its encounter");
        }
        return encounter;
    }

    private void verifyDoctorOwnsEncounter(Doctor actingDoctor, Encounter encounter) {
        if (!actingDoctor.getId().equals(encounter.getDoctorId())) {
            throw new AccessDeniedException("Doctors can only manage orders for their own encounters");
        }
    }

    private void verifyReadAccess(ClinicalOrder order, Encounter encounter, UserDetails requestingUser) {
        User currentUser = findCurrentUser(requestingUser);
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == Role.PATIENT) {
            Patient patient = findPatientByUserId(currentUser.getId());
            if (!patient.getId().equals(order.getPatientId())) {
                throw new AccessDeniedException("Patients can only view their own orders");
            }
            return;
        }
        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor doctor = findDoctorByUserId(currentUser.getId());
            if (!doctor.getId().equals(encounter.getDoctorId())) {
                throw new AccessDeniedException("Doctors can only view orders for their own encounters");
            }
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus requested) {
        boolean valid = switch (current) {
            case ORDERED -> requested == OrderStatus.IN_PROGRESS
                    || requested == OrderStatus.COMPLETED
                    || requested == OrderStatus.CANCELLED;
            case IN_PROGRESS -> requested == OrderStatus.COMPLETED || requested == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> requested == current;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid clinical order status transition from " + current + " to " + requested);
        }
    }

    private void updateStatus(ClinicalOrder order, OrderStatus status) {
        if (status == OrderStatus.COMPLETED) {
            if (order.getStatus() != OrderStatus.COMPLETED) {
                order.setCompletedAt(LocalDateTime.now());
            }
        } else {
            order.setCompletedAt(null);
        }
        order.setStatus(status);
    }

    private record OrderAndEncounter(ClinicalOrder order, Encounter encounter) {
    }
}
