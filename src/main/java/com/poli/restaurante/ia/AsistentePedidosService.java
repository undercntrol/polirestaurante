package com.poli.restaurante.ia;

import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.exception.ServicioIaException;
import com.poli.restaurante.repository.MesaRepository;
import com.poli.restaurante.repository.ProductoRepository;
import org.springframework.stereotype.Service;

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
}
