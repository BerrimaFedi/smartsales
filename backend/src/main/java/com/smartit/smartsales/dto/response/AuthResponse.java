package com.smartit.smartsales.dto.response;

import com.smartit.smartsales.domain.enums.Role;

public record AuthResponse(
        String token,
        String username,
        Role role
) {}
