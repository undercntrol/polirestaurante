package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

/** El usuario de cocina sale de la sesión (token), no del cuerpo. */
public record DisponibilidadRequest(@NotNull Boolean disponible) {
}
