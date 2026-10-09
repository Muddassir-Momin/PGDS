package com.pgds.auth;

import com.pgds.domain.Role;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 30) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String fullName,
            @Email String email) {}

    /** Used by SUPER_ADMIN to create staff accounts. */
    public record CreateStaffRequest(
            @NotBlank @Size(min = 4, max = 30) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String fullName,
            @Email String email,
            @NotNull Role role,
            Long warehouseId,
            Long fpsId) {}

    public record AuthResponse(String token, String tokenType, long expiresInMs,
                               String username, String fullName, Role role) {}
}
