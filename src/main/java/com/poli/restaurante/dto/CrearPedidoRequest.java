package com.poli.restaurante.dto;

import com.poli.restaurante.model.TipoPedido;
/** Si no se envía tipoPedido, se toma EN_SITIO. Para llevar, mesaId va vacío. El mesero sale de la sesión (token), no del cuerpo. */
public record CrearPedidoRequest(
        Long mesaId,
        TipoPedido tipoPedido) {
}
