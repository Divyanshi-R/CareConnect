package com.careconnect.dto;

import com.careconnect.entity.Role;
import com.careconnect.entity.User;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User responses.
 * Sensitive fields such as password hashes are strictly excluded from API exposure.
 */
public record UserResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
