package com.careconnect.service;

import com.careconnect.dto.AppointmentRequest;
import com.careconnect.dto.AppointmentResponse;
import com.careconnect.entity.Appointment;
import com.careconnect.entity.AppointmentStatus;
import com.careconnect.entity.Doctor;
import com.careconnect.entity.Patient;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.AppointmentRepository;
import com.careconnect.repository.DoctorRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request, UserDetails requestingUser) {
        // Validate patient exists
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));

        // Validate doctor exists
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.doctorId()));

        // Ownership enforcement: if requester is PATIENT, they can only create for their own patient profile
        if (requestingUser != null) {
            User user = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
            if (user.getRole() == Role.PATIENT && !user.getId().equals(patient.getUserId())) {
                throw new AccessDeniedException("Patients can only create appointments for themselves");
            }
        }

        // Validate date/time
        LocalDate date = request.appointmentDate();
        LocalTime time = request.appointmentTime();
        if (date == null || time == null) throw new IllegalArgumentException("appointmentDate and appointmentTime are required");
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("appointmentDate must be today or in the future");

        // Conflict check: active (non-cancelled) appointments for same doctor at same date/time
        List<Appointment> conflicts = appointmentRepository.findByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                doctor.getId(), date, time, AppointmentStatus.CANCELLED);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Doctor has an existing appointment at the requested date/time");
        }

        Appointment a = new Appointment();
        a.setPatientId(patient.getId());
        a.setDoctorId(doctor.getId());
        a.setAppointmentDate(date);
        a.setAppointmentTime(time);
        a.setReason(request.reason());
        a.setNotes(request.notes());
        a.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(a);
        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id, UserDetails requestingUser) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        // Enforce that the requesting user can view this appointment
        enforceViewPermission(a, requestingUser);
        return AppointmentResponse.fromEntity(a);
    }

    private void enforceViewPermission(Appointment a, UserDetails requestingUser) {
        if (requestingUser == null) return;
        User user = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
        if (user.getRole() == Role.ADMIN) return;

        if (user.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findById(a.getPatientId()).orElseThrow();
            if (!user.getId().equals(patient.getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
            return;
        }

        if (user.getRole() == Role.DOCTOR) {
            Doctor doc = doctorRepository.findByUserId(user.getId()).orElseThrow();
            if (!doc.getId().equals(a.getDoctorId())) {
                throw new AccessDeniedException("Access denied");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listByPatient(Long patientId, UserDetails requestingUser) {
        // Validate patient exists
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));

        if (requestingUser != null) {
            User user = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
            if (user.getRole() == Role.PATIENT && !user.getId().equals(patient.getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
        }

        return appointmentRepository.findByPatientId(patientId).stream().map(AppointmentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listByDoctor(Long doctorId, UserDetails requestingUser) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));

        if (requestingUser != null) {
            User user = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
            if (user.getRole() == Role.DOCTOR && !user.getId().equals(doctor.getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
        }

        return appointmentRepository.findByDoctorId(doctorId).stream().map(AppointmentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request, UserDetails requestingUser) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        // Only ADMIN or owning patient (for their appointment) or ADMIN can update; doctors should not change doctorId to impersonate
        if (requestingUser != null) {
            User user = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
            if (user.getRole() == Role.PATIENT && !user.getId().equals(patientRepository.findById(a.getPatientId()).orElseThrow().getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
            if (user.getRole() == Role.DOCTOR) {
                // doctor can update only their own appointments and cannot change doctorId
                Doctor doc = doctorRepository.findByUserId(user.getId()).orElseThrow();
                if (!doc.getId().equals(a.getDoctorId())) throw new AccessDeniedException("Access denied");
                if (!doc.getId().equals(request.doctorId())) throw new IllegalArgumentException("Doctors cannot change appointment doctorId");
            }
        }

        // Validate referenced patient/doctor
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.patientId()));
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.doctorId()));

        // Prevent changing ownership improperly
        if (!a.getPatientId().equals(request.patientId())) {
            throw new IllegalArgumentException("Cannot change appointment patientId");
        }

        // Validate date/time
        LocalDate date = request.appointmentDate();
        LocalTime time = request.appointmentTime();
        if (date == null || time == null) throw new IllegalArgumentException("appointmentDate and appointmentTime are required");
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("appointmentDate must be today or in the future");

        // Conflict check excluding current appointment and cancelled ones
        List<Appointment> conflicts = appointmentRepository.findByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                doctor.getId(), date, time, AppointmentStatus.CANCELLED);
        boolean conflictExists = conflicts.stream().anyMatch(c -> !c.getId().equals(a.getId()));
        if (conflictExists) throw new IllegalArgumentException("Doctor has an existing appointment at the requested date/time");

        a.setAppointmentDate(date);
        a.setAppointmentTime(time);
        a.setReason(request.reason());
        a.setNotes(request.notes());
        a.setDoctorId(doctor.getId());

        Appointment saved = appointmentRepository.save(a);
        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional
    public void cancel(Long id, UserDetails requestingUser) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        if (requestingUser != null) {
            User user = userRepository.findByEmail(requestingUser.getUsername()).orElseThrow();
            if (user.getRole() == Role.PATIENT && !user.getId().equals(patientRepository.findById(a.getPatientId()).orElseThrow().getUserId())) {
                throw new AccessDeniedException("Access denied");
            }
            if (user.getRole() == Role.DOCTOR) {
                Doctor doc = doctorRepository.findByUserId(user.getId()).orElseThrow();
                if (!doc.getId().equals(a.getDoctorId())) throw new AccessDeniedException("Access denied");
            }
        }

        a.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(a);
    }
}
