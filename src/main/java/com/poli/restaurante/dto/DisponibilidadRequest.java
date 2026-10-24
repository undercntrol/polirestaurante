package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

public record DisponibilidadRequest(
        @NotNull Boolean disponible,
        @NotNull Long cocinaId) {
}
