package com.poli.restaurante.dto;

import com.poli.restaurante.model.Producto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean disponible,
        Long categoriaId,
        String categoria) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.isDisponible(), p.getCategoria().getId(), p.getCategoria().getNombre());
    }
}
