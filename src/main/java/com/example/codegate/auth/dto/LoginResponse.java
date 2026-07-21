package com.example.codegate.auth.dto;

import com.example.codegate.user.entity.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        UserRole role,
        String userName
) {
}
