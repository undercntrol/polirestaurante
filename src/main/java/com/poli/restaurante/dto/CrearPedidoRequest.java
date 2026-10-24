package com.poli.restaurante.dto;

import com.poli.restaurante.model.TipoPedido;
import jakarta.validation.constraints.NotNull;

/** Si no se envía tipoPedido, se toma EN_SITIO. Para llevar, mesaId va vacío. */
public record CrearPedidoRequest(
        Long mesaId,
        @NotNull Long meseroId,
        TipoPedido tipoPedido) {
}
