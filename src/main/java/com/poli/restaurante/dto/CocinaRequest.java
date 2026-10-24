package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotNull;

/** Qué usuario de cocina hace la acción. Cuando haya login, esto sale del token y este DTO desaparece. */
public record CocinaRequest(@NotNull Long cocinaId) {
}
