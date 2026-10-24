package com.poli.restaurante.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Si no se envía cantidad, se toma 1. */
public record AgregarProductoRequest(
        @NotNull Long productoId,
        @Min(1) Integer cantidad,
        String notas) {
}
