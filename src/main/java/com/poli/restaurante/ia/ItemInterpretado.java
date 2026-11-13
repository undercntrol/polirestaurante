package com.poli.restaurante.ia;

/** Una línea del pedido que el intérprete entendió. notas es null si no hay. */
public record ItemInterpretado(Long productoId, String productoNombre, int cantidad, String notas) {
}
