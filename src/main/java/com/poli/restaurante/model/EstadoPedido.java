package com.poli.restaurante.model;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoPedido {
    PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO;

    /** Estados a los que se puede pasar desde este. */
    public Set<EstadoPedido> siguientesValidos() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(EN_PREPARACION, CANCELADO);
            case EN_PREPARACION -> EnumSet.of(LISTO, CANCELADO);
            case LISTO -> EnumSet.of(ENTREGADO);
            case ENTREGADO, CANCELADO -> EnumSet.noneOf(EstadoPedido.class);
        };
    }

    public boolean puedePasarA(EstadoPedido destino) {
        return siguientesValidos().contains(destino);
    }
}
