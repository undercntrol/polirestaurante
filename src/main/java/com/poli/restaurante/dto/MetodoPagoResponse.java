package com.poli.restaurante.dto;

import com.poli.restaurante.model.MetodoPago;

public record MetodoPagoResponse(Long id, String nombre) {

    public static MetodoPagoResponse de(MetodoPago m) {
        return new MetodoPagoResponse(m.getId(), m.getNombre());
    }
}
