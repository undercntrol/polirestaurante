package com.poli.restaurante.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** motor es opcional: REGLAS o GEMINI. Si no se envía, se usa GEMINI cuando está configurado. */
public record InterpretarRequest(@NotBlank @Size(max = 500) String texto, String motor) {
}
