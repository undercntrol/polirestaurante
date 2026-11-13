package com.poli.restaurante.controller;

import com.poli.restaurante.dto.InterpretarRequest;
import com.poli.restaurante.ia.AsistentePedidosService;
import com.poli.restaurante.ia.Interpretacion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Asistente de pedidos en lenguaje natural. */
@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
public class IaController {

    private final AsistentePedidosService asistente;

    /** Qué motores hay, si están listos y qué modelo de Gemini está configurado. */
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("motores", asistente.motores());
        r.put("modeloGemini", asistente.modeloGemini());
        return r;
    }

    /** Interpreta el texto y devuelve la propuesta de pedido, sin guardarla. */
    @PostMapping("/interpretar")
    public Interpretacion interpretar(@Valid @RequestBody InterpretarRequest req) {
        return asistente.interpretar(req.texto(), req.motor());
    }
}
