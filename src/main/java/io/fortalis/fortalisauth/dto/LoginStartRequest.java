package io.fortalis.fortalisauth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginStartRequest(
        @NotBlank String emailOrUsername,
        @NotBlank String password
) {}
