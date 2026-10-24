package com.poli.restaurante.service;

import com.poli.restaurante.dto.ReporteTiemposResponse;
import com.poli.restaurante.dto.ReporteVentasResponse;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.DetallePedido;
import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.HistorialEstado;
import com.poli.restaurante.model.Pedido;
import com.poli.restaurante.repository.HistorialEstadoRepository;
import com.poli.restaurante.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** HU-15: reportes de ventas y de tiempos de preparación por etapa. */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final String SIN_DATOS = "No hay pedidos en el periodo seleccionado";

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;

    /** Total vendido, número de pedidos y ticket promedio; los cancelados no suman y se cuentan aparte. */
    @Transactional(readOnly = true)
    public ReporteVentasResponse ventas(LocalDate desde, LocalDate hasta) {
        List<Pedido> pedidos = pedidosDelPeriodo(desde, hasta);
        if (pedidos.isEmpty()) {
            return new ReporteVentasResponse(desde, hasta, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, SIN_DATOS);
        }
        BigDecimal total = BigDecimal.ZERO;
        long entregados = 0;
        long cancelados = 0;
        for (Pedido p : pedidos) {
            if (p.getEstado() == EstadoPedido.ENTREGADO) {
                entregados++;
                total = total.add(totalDe(p));
            } else if (p.getEstado() == EstadoPedido.CANCELADO) {
                cancelados++;
            }
        }
        BigDecimal ticket = entregados == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(entregados), 2, RoundingMode.HALF_UP);
        return new ReporteVentasResponse(desde, hasta, total, entregados, ticket, cancelados, null);
    }

    /**
     * Promedios en minutos por etapa:
     * espera en cola (PENDIENTE → EN_PREPARACION), preparación (EN_PREPARACION → LISTO),
     * entrega (LISTO → ENTREGADO) y el total que pide la HU (PENDIENTE → LISTO).
     */
    @Transactional(readOnly = true)
    public ReporteTiemposResponse tiempos(LocalDate desde, LocalDate hasta) {
        List<Pedido> pedidos = pedidosDelPeriodo(desde, hasta);
        List<Long> pendienteAListo = new ArrayList<>();
        List<Long> espera = new ArrayList<>();
        List<Long> preparacion = new ArrayList<>();
        List<Long> entrega = new ArrayList<>();

        for (Pedido p : pedidos) {
            Map<EstadoPedido, LocalDateTime> cuando = new EnumMap<>(EstadoPedido.class);
            for (HistorialEstado h : historialRepository.findByPedidoIdOrderByFechaAscIdAsc(p.getId())) {
                cuando.putIfAbsent(h.getEstado(), h.getFecha());
            }
            agregar(pendienteAListo, cuando, EstadoPedido.PENDIENTE, EstadoPedido.LISTO);
            agregar(espera, cuando, EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION);
            agregar(preparacion, cuando, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);
            agregar(entrega, cuando, EstadoPedido.LISTO, EstadoPedido.ENTREGADO);
        }
        String mensaje = pedidos.isEmpty() ? SIN_DATOS
                : pendienteAListo.isEmpty() ? "Ningún pedido del periodo ha llegado a LISTO todavía" : null;
        return new ReporteTiemposResponse(desde, hasta, pendienteAListo.size(),
                promedioMinutos(pendienteAListo), promedioMinutos(espera),
                promedioMinutos(preparacion), promedioMinutos(entrega), mensaje);
    }

    private List<Pedido> pedidosDelPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        // Rango de días completos: desde las 00:00 de 'desde' hasta antes de las 00:00 del día siguiente a 'hasta'
        return pedidoRepository.findByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    private BigDecimal totalDe(Pedido p) {
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido d : p.getDetalles()) {
            total = total.add(d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
        }
        return total;
    }

    /** Guarda la duración en segundos entre dos estados, si el pedido pasó por ambos. */
    private void agregar(List<Long> destino, Map<EstadoPedido, LocalDateTime> cuando,
                         EstadoPedido desde, EstadoPedido hasta) {
        LocalDateTime a = cuando.get(desde);
        LocalDateTime b = cuando.get(hasta);
        if (a != null && b != null) {
            destino.add(Duration.between(a, b).getSeconds());
        }
    }

    /** Promedio en minutos con un decimal; null si no hay mediciones. */
    private Double promedioMinutos(List<Long> segundos) {
        if (segundos.isEmpty()) {
            return null;
        }
        double promedio = segundos.stream().mapToLong(Long::longValue).average().orElse(0) / 60.0;
        return Math.round(promedio * 10) / 10.0;
    }
}
