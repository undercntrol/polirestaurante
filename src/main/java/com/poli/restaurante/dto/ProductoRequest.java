package com.poli.restaurante.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** HU-11: datos para crear o editar un producto. Si no se envía disponible, se toma true. */
public record ProductoRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @DecimalMin("0.01") BigDecimal precio,
        @NotNull Long categoriaId,
        Boolean disponible) {
}
