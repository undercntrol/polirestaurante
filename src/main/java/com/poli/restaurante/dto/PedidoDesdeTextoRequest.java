package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El mesero sale de la sesión (token), no del cuerpo. */
public record PedidoDesdeTextoRequest(
        @NotBlank @Size(max = 500) String texto,
        String motor) {
}
