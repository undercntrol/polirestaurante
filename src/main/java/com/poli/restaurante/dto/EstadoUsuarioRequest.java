package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoUsuarioRequest(@NotNull Boolean activo) {
}
