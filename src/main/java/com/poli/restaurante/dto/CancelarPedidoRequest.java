package com.poli.restaurante.dto;

import jakarta.validation.constraints.Size;

/**
 * HU-07: motivo de la cancelación. Es opcional para el mesero y obligatorio para cocina,
 * porque el mesero necesita saber qué pasó para avisarle al cliente.
 */
public record CancelarPedidoRequest(@Size(max = 200) String motivo) {
}
