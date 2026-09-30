package com.careconnect.dto;

import com.careconnect.entity.Role;

public record LoginResponse(
        String token,
        Long userId,
        String email,
        Role role
) {
}
