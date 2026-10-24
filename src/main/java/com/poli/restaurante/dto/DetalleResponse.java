package com.poli.restaurante.dto;

import com.poli.restaurante.model.DetallePedido;

import java.math.BigDecimal;

public record DetalleResponse(
        Long id,
        Long productoId,
        String productoNombre,
        int cantidad,
        String notas,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {

    public static DetalleResponse de(DetallePedido d) {
        BigDecimal subtotal = d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()));
        return new DetalleResponse(d.getId(), d.getProducto().getId(), d.getProducto().getNombre(),
                d.getCantidad(), d.getNotas(), d.getPrecioUnitario(), subtotal);
    }
}
