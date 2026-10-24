package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

/** Qué mesero hace la acción. Cuando haya login, esto sale del token y este DTO desaparece. */
public record MeseroRequest(@NotNull Long meseroId) {
}
