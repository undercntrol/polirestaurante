package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

/** El mesero sale de la sesión (token), no del cuerpo. */
public record RegistrarPagoRequest(@NotNull Long metodoPagoId) {
}
