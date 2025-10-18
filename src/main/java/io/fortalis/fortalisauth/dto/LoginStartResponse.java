package io.fortalis.fortalisauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LoginStartResponse(
        @NotBlank String loginTicket,
        @NotEmpty @JsonProperty("allowedFactors") List<String> allowedFactors
) {
}
