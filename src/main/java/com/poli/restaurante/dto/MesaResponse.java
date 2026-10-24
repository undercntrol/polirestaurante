package com.poli.restaurante.dto;

import com.poli.restaurante.model.EstadoMesa;
import com.poli.restaurante.model.Mesa;

public record MesaResponse(Long id, Integer numero, EstadoMesa estado) {

    public static MesaResponse de(Mesa m) {
        return new MesaResponse(m.getId(), m.getNumero(), m.getEstado());
    }
}
