package com.poli.restaurante.dto;

import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        Long mesaId,
        Integer mesaNumero,
        Long meseroId,
        String meseroNombre,
        EstadoPedido estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaUltimaActualizacion,
        String metodoPago,
        boolean pagado,
        BigDecimal total,
        List<DetalleResponse> detalles) {

    /** Debe llamarse dentro de una transacción (las relaciones son LAZY). */
    public static PedidoResponse de(Pedido p) {
        List<DetalleResponse> detalles = p.getDetalles().stream().map(DetalleResponse::de).toList();
        BigDecimal total = detalles.stream().map(DetalleResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PedidoResponse(p.getId(), p.getMesa().getId(), p.getMesa().getNumero(),
                p.getMesero().getId(), p.getMesero().getNombre(), p.getEstado(),
                p.getFechaCreacion(), p.getFechaUltimaActualizacion(),
                p.getMetodoPago(), p.isPagado(), total, detalles);
    }
}
