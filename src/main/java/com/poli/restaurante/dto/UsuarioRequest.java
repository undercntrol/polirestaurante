package com.poli.restaurante.dto;

import com.poli.restaurante.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HU-14: el rol decide en qué tabla queda el usuario (mesero, cocina o administrador). */
public record UsuarioRequest(
        @NotBlank String nombre,
        @NotBlank @Size(max = 40) String username,
        @NotBlank @Size(min = 4) String password,
        @NotNull Rol rol) {
}
