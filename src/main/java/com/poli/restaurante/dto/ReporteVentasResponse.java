package com.poli.restaurante.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * HU-15: ventas del periodo. Solo cuentan los pedidos ENTREGADOS; los cancelados van aparte.
 * mensaje viene lleno cuando no hay pedidos en el rango.
 */
public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal totalVendido,
        long numeroPedidos,
        BigDecimal ticketPromedio,
        long pedidosCancelados,
        String mensaje) {
}
