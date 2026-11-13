package com.poli.restaurante.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemConfirmado(@NotNull Long productoId, @NotNull @Min(1) Integer cantidad, String notas) {
}
