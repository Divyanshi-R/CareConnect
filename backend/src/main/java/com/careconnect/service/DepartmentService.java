package com.careconnect.service;

import com.careconnect.dto.DepartmentRequest;
import com.careconnect.dto.DepartmentResponse;
import com.careconnect.entity.Department;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Department with this name already exists");
        }
        Department d = new Department();
        d.setName(request.name());
        d.setDescription(request.description());
        Department saved = departmentRepository.save(d);
        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream().map(DepartmentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return DepartmentResponse.fromEntity(d);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        if (!d.getName().equals(request.name()) && departmentRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Department with this name already exists");
        }
        d.setName(request.name());
        d.setDescription(request.description());
        Department saved = departmentRepository.save(d);
        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(Long id) {
        Department d = departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        departmentRepository.delete(d);
    }
}
