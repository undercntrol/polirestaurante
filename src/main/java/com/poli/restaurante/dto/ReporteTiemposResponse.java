package com.poli.restaurante.dto;

import java.time.LocalDate;

/**
 * HU-15: tiempos promedio en minutos, calculados con el historial de estados.
 * Cada promedio usa solo los pedidos que pasaron por los dos estados de esa etapa; es null si no hay ninguno.
 */
public record ReporteTiemposResponse(
        LocalDate desde,
        LocalDate hasta,
        long pedidosMedidos,
        Double promedioPendienteAListo,
        Double promedioEsperaEnCola,
        Double promedioPreparacion,
        Double promedioEntrega,
        String mensaje) {
}
