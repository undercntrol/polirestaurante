package com.poli.restaurante.dto;

import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.HistorialEstado;

import java.time.LocalDateTime;

/** HU-10: un cambio de estado con quién lo hizo (tipo de actor, id en su tabla y nombre). */
public record HistorialResponse(
        Long id,
        EstadoPedido estado,
        LocalDateTime fecha,
        String actorTipo,
        Long actorId,
        String actorNombre) {

    /** Debe llamarse dentro de una transacción (las relaciones son LAZY). */
    public static HistorialResponse de(HistorialEstado h) {
        if (h.getMesero() != null) {
            return new HistorialResponse(h.getId(), h.getEstado(), h.getFecha(),
                    "MESERO", h.getMesero().getId(), h.getMesero().getNombre());
        }
        if (h.getCocina() != null) {
            return new HistorialResponse(h.getId(), h.getEstado(), h.getFecha(),
                    "COCINA", h.getCocina().getId(), h.getCocina().getNombre());
        }
        if (h.getCliente() != null) {
            return new HistorialResponse(h.getId(), h.getEstado(), h.getFecha(),
                    "CLIENTE", h.getCliente().getId(), h.getCliente().getNombre());
        }
        return new HistorialResponse(h.getId(), h.getEstado(), h.getFecha(), null, null, null);
    }
}
