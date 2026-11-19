package com.poli.restaurante.dto;

import com.poli.restaurante.model.TipoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * La propuesta del asistente, ya revisada por el mesero. Para un pedido en sitio se indica el
 * número de mesa (no su id), que es lo que entiende la IA. El mesero sale de la sesión (token), no del cuerpo.
 */
public record ConfirmarPedidoIaRequest(
        TipoPedido tipoPedido,
        Integer mesaNumero,
        @NotEmpty @Valid List<ItemConfirmado> items) {
}
