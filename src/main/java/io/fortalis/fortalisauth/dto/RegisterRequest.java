package io.fortalis.fortalisauth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 200) String password,
        @NotBlank @Size(min = 3, max = 32) String displayName
) {
    public RegisterRequest {
        email = email != null ? email.toLowerCase() : null;
    }
}

