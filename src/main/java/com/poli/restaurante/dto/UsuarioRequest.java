package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

/** Quién hace la acción. Cuando haya login, esto sale del token y este DTO desaparece. */
public record UsuarioRequest(@NotNull Long usuarioId) {
}
