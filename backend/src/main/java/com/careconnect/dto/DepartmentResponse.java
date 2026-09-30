package com.careconnect.dto;

import com.careconnect.entity.Department;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DepartmentResponse fromEntity(Department d) {
        if (d == null) return null;
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
