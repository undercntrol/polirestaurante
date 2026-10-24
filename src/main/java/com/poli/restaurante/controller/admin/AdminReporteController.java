package com.poli.restaurante.controller.admin;

import com.poli.restaurante.dto.ReporteTiemposResponse;
import com.poli.restaurante.dto.ReporteVentasResponse;
import com.poli.restaurante.service.AccesoService;
import com.poli.restaurante.service.AdminFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** HU-15: reportes. Fechas en formato AAAA-MM-DD, ambas incluidas. */
@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReporteController {

    private final AdminFacade facade;

    /** Ej.: GET /api/admin/reportes/ventas?desde=2026-10-01&hasta=2026-10-31 */
    @GetMapping("/ventas")
    public ReporteVentasResponse ventas(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return facade.reporteVentas(adminId, desde, hasta);
    }

    /** Ej.: GET /api/admin/reportes/tiempos?desde=2026-10-01&hasta=2026-10-31 */
    @GetMapping("/tiempos")
    public ReporteTiemposResponse tiempos(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return facade.reporteTiempos(adminId, desde, hasta);
    }
}
