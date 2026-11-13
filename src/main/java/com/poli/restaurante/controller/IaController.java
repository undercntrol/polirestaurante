package com.poli.restaurante.controller;

import com.poli.restaurante.dto.InterpretarRequest;
import com.poli.restaurante.ia.AsistentePedidosService;
import com.poli.restaurante.ia.Interpretacion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Asistente de pedidos en lenguaje natural. */
@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
public class IaController {

    private final AsistentePedidosService asistente;

    /** Qué motores hay y si están listos. Ej.: {"REGLAS": true, "GEMINI": false} */
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of("motores", asistente.motores());
    }

    /** Interpreta el texto y devuelve la propuesta de pedido, sin guardarla. */
    @PostMapping("/interpretar")
    public Interpretacion interpretar(@Valid @RequestBody InterpretarRequest req) {
        return asistente.interpretar(req.texto(), req.motor());
    }
}
