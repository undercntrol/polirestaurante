package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(@NotBlank String nombre) {
}
