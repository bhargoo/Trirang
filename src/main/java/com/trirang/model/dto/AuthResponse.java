package com.trirang.model.dto;

import com.trirang.model.enums.shared.Role;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String name,
    String email,
    Role role,
    Integer trustScore
) {}
