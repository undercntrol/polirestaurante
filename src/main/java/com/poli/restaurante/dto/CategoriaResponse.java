package com.poli.restaurante.dto;

import com.poli.restaurante.model.Categoria;

public record CategoriaResponse(Long id, String nombre, long cantidadProductos) {

    public static CategoriaResponse de(Categoria c, long cantidadProductos) {
        return new CategoriaResponse(c.getId(), c.getNombre(), cantidadProductos);
    }
}
