package io.fortalis.fortalisauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LoginStartResponse(
        @NotBlank String loginTicket,
        @NotEmpty List<String> mfaMethods
) {
}
