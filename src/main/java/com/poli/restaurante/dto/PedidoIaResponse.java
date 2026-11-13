package com.poli.restaurante.dto;

import com.poli.restaurante.ia.Interpretacion;

/** Lo que entendió el asistente y el pedido que quedó creado con eso. */
public record PedidoIaResponse(Interpretacion interpretacion, PedidoResponse pedido) {
}
