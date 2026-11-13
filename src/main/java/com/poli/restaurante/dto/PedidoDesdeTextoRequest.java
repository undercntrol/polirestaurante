package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PedidoDesdeTextoRequest(
        @NotBlank @Size(max = 500) String texto,
        @NotNull Long meseroId,
        String motor) {
}
