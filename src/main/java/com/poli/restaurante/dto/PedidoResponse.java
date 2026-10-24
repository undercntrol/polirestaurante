package com.poli.restaurante.dto;

import com.poli.restaurante.model.EstadoPago;
import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.Pedido;
import com.poli.restaurante.model.TipoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        TipoPedido tipoPedido,
        Long mesaId,
        Integer mesaNumero,
        Long meseroId,
        String meseroNombre,
        EstadoPedido estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaUltimaActualizacion,
        String metodoPago,
        EstadoPago estadoPago,
        BigDecimal total,
        List<DetalleResponse> detalles) {

    /** Debe llamarse dentro de una transacción (las relaciones son LAZY). */
    public static PedidoResponse de(Pedido p) {
        List<DetalleResponse> detalles = p.getDetalles().stream().map(DetalleResponse::de).toList();
        BigDecimal total = detalles.stream().map(DetalleResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Long mesaId = p.getMesa() == null ? null : p.getMesa().getId();
        Integer mesaNumero = p.getMesa() == null ? null : p.getMesa().getNumero();
        return new PedidoResponse(p.getId(), p.getTipoPedido(), mesaId, mesaNumero,
                p.getMesero().getId(), p.getMesero().getNombre(), p.getEstado(),
                p.getFechaCreacion(), p.getFechaUltimaActualizacion(),
                p.getMetodoPago() == null ? null : p.getMetodoPago().getNombre(), p.getEstadoPago(),
                total, detalles);
    }
}
