package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

public record CrearPedidoRequest(
        @NotNull Long mesaId,
        @NotNull Long meseroId) {
}
