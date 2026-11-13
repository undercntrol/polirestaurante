package com.poli.restaurante.ia;

import com.poli.restaurante.model.TipoPedido;

import java.util.List;

/**
 * Resultado de interpretar un texto: qué entendió el motor (REGLAS o GEMINI),
 * cuánto tardó y qué advertencias dejó (productos que no existen, datos que faltan...).
 * Es solo una propuesta: el mesero la revisa antes de crear el pedido.
 */
public record Interpretacion(
        String motor,
        TipoPedido tipoPedido,
        Integer mesaNumero,
        List<ItemInterpretado> items,
        List<String> advertencias,
        long milisegundos) {

    public Interpretacion conTiempo(long ms) {
        return new Interpretacion(motor, tipoPedido, mesaNumero, items, advertencias, ms);
    }
}
