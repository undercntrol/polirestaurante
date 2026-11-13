package com.poli.restaurante.ia;

import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.exception.ServicioIaException;
import com.poli.restaurante.repository.MesaRepository;
import com.poli.restaurante.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Asistente de pedidos: recibe lo que dicta el mesero en lenguaje natural y lo convierte en una
 * propuesta de pedido usando uno de los motores disponibles (REGLAS o GEMINI).
 * La propuesta no se guarda: el mesero la revisa y la confirma.
 */
@Service
public class AsistentePedidosService {

    private final Map<String, InterpretePedido> interpretes = new LinkedHashMap<>();
    private final ProductoRepository productoRepository;
    private final MesaRepository mesaRepository;

    public AsistentePedidosService(List<InterpretePedido> interpretes, ProductoRepository productoRepository,
                                   MesaRepository mesaRepository) {
        for (InterpretePedido i : interpretes) {
            this.interpretes.put(i.nombre(), i);
        }
        this.productoRepository = productoRepository;
        this.mesaRepository = mesaRepository;
    }

    /** Modelo de Gemini configurado (por ejemplo gemini-3.8-flash), o null si no hay. */
    public String modeloGemini() {
        return interpretes.get("GEMINI") instanceof InterpreteGemini g ? g.modelo() : null;
    }

    /** Motores registrados y si están listos para usarse. */
    public Map<String, Boolean> motores() {
        Map<String, Boolean> m = new LinkedHashMap<>();
        interpretes.forEach((nombre, i) -> m.put(nombre, i.disponible()));
        return m;
    }

    /** Productos que se pueden pedir ahora (los agotados no se le muestran al intérprete). */
    public List<ItemMenu> menuDisponible() {
        return productoRepository.findByDisponibleTrue().stream()
                .map(p -> new ItemMenu(p.getId(), p.getNombre()))
                .toList();
    }

    public Interpretacion interpretar(String texto, String motor) {
        InterpretePedido interprete = elegir(motor);
        try {
            return revisarMesa(interprete.interpretar(texto, menuDisponible()));
        } catch (IllegalStateException e) {
            throw new ServicioIaException(e.getMessage());
        }
    }

    /** Sin motor indicado se usa GEMINI si está configurado; si no, REGLAS. */
    public InterpretePedido elegir(String motor) {
        if (motor == null || motor.isBlank()) {
            InterpretePedido gemini = interpretes.get("GEMINI");
            return gemini != null && gemini.disponible() ? gemini : interpretes.get("REGLAS");
        }
        InterpretePedido i = interpretes.get(motor.trim().toUpperCase());
        if (i == null) {
            throw new ReglaNegocioException("Motor no válido: " + motor + ". Opciones: " + interpretes.keySet());
        }
        if (!i.disponible()) {
            throw new ReglaNegocioException("El motor " + i.nombre()
                    + " no está configurado: falta GEMINI_API_KEY en el archivo .env");
        }
        return i;
    }

    /** Agrega una advertencia si la mesa que entendió el intérprete no existe en el restaurante. */
    private Interpretacion revisarMesa(Interpretacion r) {
        if (r.mesaNumero() == null || mesaRepository.existsByNumero(r.mesaNumero())) {
            return r;
        }
        List<String> advertencias = new ArrayList<>(r.advertencias());
        advertencias.add("La mesa " + r.mesaNumero() + " no existe");
        return new Interpretacion(r.motor(), r.tipoPedido(), r.mesaNumero(), r.items(), advertencias, r.milisegundos());
    }

    // ---------- Evaluación (para el artículo) ----------

    /**
     * Pasa el conjunto de prueba (resources/ia/casos-prueba.json) por uno o por todos los motores.
     * El conjunto trae su propio menú, así que el resultado no depende de lo que haya en la base de datos.
     *
     * @param motor    REGLAS, GEMINI o AMBOS (por defecto AMBOS; si Gemini no tiene clave, solo REGLAS)
     * @param pausaMs  espera entre frases para Gemini, para no pasar el límite gratuito por minuto
     * @param maxCasos cuántas frases usar (0 = todas), por si el límite diario es bajo
     */
    public List<Evaluador.Resultado> evaluar(String motor, long pausaMs, int maxCasos) {
        Evaluador.Conjunto conjunto = cargarConjunto();
        if (maxCasos > 0 && maxCasos < conjunto.casos().size()) {
            conjunto = new Evaluador.Conjunto(conjunto.menu(), conjunto.casos().subList(0, maxCasos));
        }
        List<InterpretePedido> motores = new ArrayList<>();
        if (motor == null || motor.isBlank() || motor.equalsIgnoreCase("AMBOS")) {
            interpretes.values().stream().filter(InterpretePedido::disponible).forEach(motores::add);
        } else {
            motores.add(elegir(motor));
        }
        List<Evaluador.Resultado> resultados = new ArrayList<>();
        for (InterpretePedido i : motores) {
            long pausa = i.nombre().equals("REGLAS") ? 0 : Math.max(0, pausaMs);
            resultados.add(Evaluador.evaluar(i, conjunto, pausa));
        }
        return resultados;
    }

    private Evaluador.Conjunto cargarConjunto() {
        try (InputStream in = getClass().getResourceAsStream("/ia/casos-prueba.json")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró ia/casos-prueba.json en los recursos");
            }
            return Evaluador.leerConjunto(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
