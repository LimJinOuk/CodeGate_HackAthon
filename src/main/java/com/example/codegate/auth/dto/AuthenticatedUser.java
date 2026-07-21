package com.example.codegate.auth.dto;

import com.example.codegate.user.entity.UserRole;

public record AuthenticatedUser(
        Long userId,
        UserRole role
) {
}
