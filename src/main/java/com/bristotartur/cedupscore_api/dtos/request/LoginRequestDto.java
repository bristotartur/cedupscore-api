package com.bristotartur.cedupscore_api.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank String email,
        @NotBlank String password
) {
}
