package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

public record RegistrarPagoRequest(
        @NotNull Long metodoPagoId,
        @NotNull Long meseroId) {
}
