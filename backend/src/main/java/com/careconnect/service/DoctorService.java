package com.careconnect.service;

import com.careconnect.dto.DoctorRequest;
import com.careconnect.dto.DoctorResponse;
import com.careconnect.entity.Doctor;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.DepartmentRepository;
import com.careconnect.repository.DoctorRepository;
import com.careconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository, DepartmentRepository departmentRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DoctorResponse create(DoctorRequest req) {
        // validate user
        User user = userRepository.findById(req.userId()).orElseThrow(() -> new ResourceNotFoundException("User", "id", req.userId()));
        if (user.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Doctor must be associated with a User having DOCTOR role");
        }

        // validate department
        departmentRepository.findById(req.departmentId()).orElseThrow(() -> new ResourceNotFoundException("Department", "id", req.departmentId()));

        // unique license
        if (doctorRepository.existsByLicenseNumber(req.licenseNumber())) {
            throw new IllegalArgumentException("Doctor with this license number already exists");
        }

        Doctor d = new Doctor();
        d.setUserId(req.userId());
        d.setDepartmentId(req.departmentId());
        d.setFirstName(req.firstName());
        d.setLastName(req.lastName());
        d.setSpecialization(req.specialization());
        d.setLicenseNumber(req.licenseNumber());
        d.setPhone(req.phone());

        Doctor saved = doctorRepository.save(d);
        return DoctorResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> listAll() {
        return doctorRepository.findAll().stream().map(DoctorResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> findByDepartment(Long departmentId) {
        departmentRepository.findById(departmentId).orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
        return doctorRepository.findByDepartmentId(departmentId).stream().map(DoctorResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DoctorResponse getById(Long id) {
        Doctor d = doctorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        return DoctorResponse.fromEntity(d);
    }

    @Transactional
    public DoctorResponse update(Long id, DoctorRequest req) {
        Doctor d = doctorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        // Prevent changing ownership
        if (!d.getUserId().equals(req.userId())) {
            throw new IllegalArgumentException("Cannot change doctor ownership/userId");
        }

        departmentRepository.findById(req.departmentId()).orElseThrow(() -> new ResourceNotFoundException("Department", "id", req.departmentId()));

        if (!d.getLicenseNumber().equals(req.licenseNumber()) && doctorRepository.existsByLicenseNumber(req.licenseNumber())) {
            throw new IllegalArgumentException("Doctor with this license number already exists");
        }

        d.setDepartmentId(req.departmentId());
        d.setFirstName(req.firstName());
        d.setLastName(req.lastName());
        d.setSpecialization(req.specialization());
        d.setLicenseNumber(req.licenseNumber());
        d.setPhone(req.phone());

        Doctor saved = doctorRepository.save(d);
        return DoctorResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(Long id) {
        Doctor d = doctorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        doctorRepository.delete(d);
    }
}
