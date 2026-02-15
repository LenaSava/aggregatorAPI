package com.project.aggregator.model;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductRequest(

        @NotBlank(message = "market is required (e.g. nl-NL, de-DE, pl-PL)")
        @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "market must be a valid BCP-47 locale tag (e.g. nl-NL, de-DE, pl-PL)")
        String market,

        @Pattern(regexp = "^[\\w-]+$")
        @Nullable
        String customerId
) {
}