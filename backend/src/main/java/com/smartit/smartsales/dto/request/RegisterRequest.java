package com.smartit.smartsales.dto.request;

import com.smartit.smartsales.domain.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotNull Role role
) {}
