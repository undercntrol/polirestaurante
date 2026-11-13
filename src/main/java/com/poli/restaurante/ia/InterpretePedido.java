package com.poli.restaurante.ia;

import java.util.List;

/** Convierte un pedido escrito en lenguaje natural en una Interpretacion. */
public interface InterpretePedido {

    /** Nombre del motor: REGLAS o GEMINI. */
    String nombre();

    /** false si al motor le falta configuración (por ejemplo, la clave de la API). */
    boolean disponible();

    Interpretacion interpretar(String texto, List<ItemMenu> menu);
}
