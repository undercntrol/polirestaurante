package com.poli.restaurante.controller;

import com.poli.restaurante.dto.*;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.ia.AsistentePedidosService;
import com.poli.restaurante.ia.ConfirmacionPedidoIaService;
import com.poli.restaurante.ia.Evaluador;
import com.poli.restaurante.ia.Interpretacion;
import com.poli.restaurante.ia.ItemInterpretado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Asistente de pedidos en lenguaje natural. */
@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
public class IaController {

    private final AsistentePedidosService asistente;
    private final ConfirmacionPedidoIaService confirmacion;

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

    /** El mesero revisó la propuesta (y quizá la corrigió): se crea el pedido. */
    @PostMapping("/pedidos/confirmar")
    public ResponseEntity<PedidoResponse> confirmar(@Valid @RequestBody ConfirmarPedidoIaRequest req) {
        PedidoResponse pedido = confirmacion.confirmar(req.meseroId(), req.tipoPedido(), req.mesaNumero(), req.items());
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    /** Atajo para Postman: interpreta el texto y crea el pedido de una vez, sin paso de revisión. */
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoIaResponse> crearDesdeTexto(@Valid @RequestBody PedidoDesdeTextoRequest req) {
        Interpretacion i = asistente.interpretar(req.texto(), req.motor());
        if (i.items().isEmpty()) {
            throw new ReglaNegocioException("No se pudo armar el pedido: " + String.join("; ", i.advertencias()));
        }
        List<ItemConfirmado> items = i.items().stream()
                .map((ItemInterpretado it) -> new ItemConfirmado(it.productoId(), it.cantidad(), it.notas()))
                .toList();
        PedidoResponse pedido = confirmacion.confirmar(req.meseroId(), i.tipoPedido(), i.mesaNumero(), items);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PedidoIaResponse(i, pedido));
    }

    /**
     * Evalúa los motores con el conjunto de prueba y devuelve las métricas (exactitud, precisión,
     * exhaustividad, F1, latencia) y el detalle de cada frase. Con Gemini tarda unos minutos.
     * Ej.: POST /api/ia/evaluacion?motor=AMBOS&pausaMs=6000
     */
    @PostMapping("/evaluacion")
    public Map<String, Object> evaluar(@RequestParam(defaultValue = "AMBOS") String motor,
                                       @RequestParam(defaultValue = "6000") long pausaMs,
                                       @RequestParam(defaultValue = "0") int maxCasos) {
        List<Evaluador.Resultado> resultados = asistente.evaluar(motor, pausaMs, maxCasos);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("modeloGemini", asistente.modeloGemini());
        r.put("fecha", java.time.LocalDateTime.now().toString());
        r.put("resultados", resultados);
        return r;
    }
}
