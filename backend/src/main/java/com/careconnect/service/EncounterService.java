package com.careconnect.service;

import com.careconnect.dto.EncounterRequest;
import com.careconnect.dto.EncounterResponse;
import com.careconnect.entity.AuditAction;
import com.careconnect.entity.*;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public EncounterService(
            EncounterRepository encounterRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EncounterResponse create(EncounterRequest request, UserDetails requestingUser) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.doctorId()));

        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", request.appointmentId()));

        if (requestingUser != null) {
            User currentUser = userRepository.findByEmail(requestingUser.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));

            if (currentUser.getRole() == Role.PATIENT) {
                Patient currentPatient = patientRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", currentUser.getId()));

                if (!currentPatient.getId().equals(patient.getId())) {
                    throw new AccessDeniedException("Patients can only create encounters for themselves");
                }

                if (!appointment.getPatientId().equals(currentPatient.getId())) {
                    throw new AccessDeniedException("Patients can only create encounters for their own appointments");
                }
            }

            if (currentUser.getRole() == Role.DOCTOR) {
                Doctor actingDoctor = doctorRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied"));
                if (!actingDoctor.getId().equals(doctor.getId())) {
                    throw new AccessDeniedException("Doctors can only create encounters for their own appointments");
                }
            }
        }

        if (!appointment.getPatientId().equals(patient.getId())) {
            throw new IllegalArgumentException("Appointment does not belong to the specified patient");
        }

        if (!appointment.getDoctorId().equals(doctor.getId())) {
            throw new IllegalArgumentException("Appointment does not belong to the specified doctor");
        }

        if (encounterRepository.existsByAppointmentId(request.appointmentId())) {
            throw new IllegalArgumentException("An encounter already exists for this appointment");
        }

        Encounter encounter = new Encounter();
        encounter.setPatientId(patient.getId());
        encounter.setDoctorId(doctor.getId());
        encounter.setAppointmentId(appointment.getId());
        encounter.setEncounterDate(request.encounterDate());
        encounter.setChiefComplaint(request.chiefComplaint());
        encounter.setDiagnosis(request.diagnosis());
        encounter.setTreatmentPlan(request.treatmentPlan() == null ? "" : request.treatmentPlan());
        encounter.setNotes(request.notes());
        encounter.setVitals(request.vitals());

        Encounter saved = encounterRepository.save(encounter);
        auditLogService.record(AuditAction.ENCOUNTER_CREATED, requestingUser, "Encounter", saved.getId());
        return EncounterResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public EncounterResponse getById(Long id, UserDetails requestingUser) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", id));

        enforceAccess(encounter, requestingUser);
        return EncounterResponse.fromEntity(encounter);
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> listByPatient(Long patientId, UserDetails requestingUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));

        User currentUser = null;
        if (requestingUser != null) {
            currentUser = userRepository.findByEmail(requestingUser.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));

            if (currentUser.getRole() == Role.PATIENT && !currentUser.getId().equals(patient.getUserId())) {
                throw new AccessDeniedException("Access denied");
            }

            if (currentUser.getRole() == Role.DOCTOR) {
                Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied"));
                return encounterRepository.findByPatientId(patientId).stream()
                        .filter(encounter -> encounter.getDoctorId().equals(doctor.getId()))
                        .map(EncounterResponse::fromEntity)
                        .collect(Collectors.toList());
            }
        }

        return encounterRepository.findByPatientId(patientId).stream()
                .map(EncounterResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> listByDoctor(Long doctorId, UserDetails requestingUser) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));

        if (requestingUser != null) {
            User currentUser = userRepository.findByEmail(requestingUser.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));

            if (currentUser.getRole() == Role.PATIENT) {
                Patient patient = patientRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", currentUser.getId()));
                return encounterRepository.findByDoctorId(doctorId).stream()
                        .filter(encounter -> encounter.getPatientId().equals(patient.getId()))
                        .map(EncounterResponse::fromEntity)
                        .collect(Collectors.toList());
            }

            if (currentUser.getRole() == Role.DOCTOR) {
                Doctor actingDoctor = doctorRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied"));
                if (!actingDoctor.getId().equals(doctor.getId())) {
                    throw new AccessDeniedException("Access denied");
                }
            }
        }

        return encounterRepository.findByDoctorId(doctorId).stream()
                .map(EncounterResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EncounterResponse update(Long id, EncounterRequest request, UserDetails requestingUser) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter", "id", id));

        if (requestingUser != null) {
            User currentUser = userRepository.findByEmail(requestingUser.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));

            if (currentUser.getRole() == Role.PATIENT) {
                Patient patient = patientRepository.findById(encounter.getPatientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", encounter.getPatientId()));
                if (!currentUser.getId().equals(patient.getUserId())) {
                    throw new AccessDeniedException("Access denied");
                }
                if (!encounter.getPatientId().equals(request.patientId())) {
                    throw new AccessDeniedException("Patients can only update their own encounter");
                }
            }

            if (currentUser.getRole() == Role.DOCTOR) {
                Doctor actingDoctor = doctorRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Access denied"));
                if (!actingDoctor.getId().equals(encounter.getDoctorId())) {
                    throw new AccessDeniedException("Access denied");
                }
            }
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.doctorId()));
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", request.appointmentId()));

        if (!encounter.getPatientId().equals(request.patientId())) {
            throw new IllegalArgumentException("Cannot change encounter patientId");
        }

        if (!encounter.getDoctorId().equals(request.doctorId())) {
            throw new IllegalArgumentException("Cannot change encounter doctorId");
        }

        if (!encounter.getAppointmentId().equals(request.appointmentId())) {
            throw new IllegalArgumentException("Cannot change encounter appointmentId");
        }

        if (!appointment.getPatientId().equals(patient.getId()) || !appointment.getDoctorId().equals(doctor.getId())) {
            throw new IllegalArgumentException("Appointment must belong to the same patient and doctor");
        }

        if (encounterRepository.existsByAppointmentId(request.appointmentId()) && !request.appointmentId().equals(encounter.getAppointmentId())) {
            throw new IllegalArgumentException("An encounter already exists for this appointment");
        }

        encounter.setEncounterDate(request.encounterDate());
        encounter.setChiefComplaint(request.chiefComplaint());
        encounter.setDiagnosis(request.diagnosis());
        encounter.setTreatmentPlan(request.treatmentPlan() == null ? "" : request.treatmentPlan());
        encounter.setNotes(request.notes());
        encounter.setVitals(request.vitals());

        Encounter saved = encounterRepository.save(encounter);
        return EncounterResponse.fromEntity(saved);
    }

    private void enforceAccess(Encounter encounter, UserDetails requestingUser) {
        if (requestingUser == null) {
            return;
        }

        User currentUser = userRepository.findByEmail(requestingUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requestingUser.getUsername()));

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findById(encounter.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", encounter.getPatientId()));
            if (!currentUser.getId().equals(patient.getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
            return;
        }

        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            if (!doctor.getId().equals(encounter.getDoctorId())) {
                throw new AccessDeniedException("Access denied");
            }
        }
    }
}
