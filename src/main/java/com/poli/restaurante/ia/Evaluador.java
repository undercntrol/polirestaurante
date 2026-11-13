package com.poli.restaurante.ia;

import com.poli.restaurante.model.TipoPedido;

import java.util.*;

/**
 * Mide qué tan bien entiende un intérprete un conjunto de frases con su resultado esperado.
 *
 * Métricas:
 * - Pedidos exactos: tipo, mesa y productos con sus cantidades iguales a lo esperado.
 * - Precisión: de las unidades que propuso el intérprete, cuántas eran correctas.
 * - Exhaustividad (recall): de las unidades esperadas, cuántas encontró.
 * - F1: media armónica de las dos anteriores.
 * - Mesa correcta y tipo correcto: porcentaje de frases donde acertó cada dato.
 * - Notas correctas: de las líneas que debían llevar nota, en cuántas la capturó.
 * - Latencia: tiempo promedio y máximo por frase.
 */
public final class Evaluador {

    private static final Set<String> VACIAS_NOTA = Set.of("y", "de", "la", "el", "a", "un", "una", "con", ",");

    private Evaluador() {
    }

    public record ItemEsperado(String producto, int cantidad, String notas) {
    }

    public record Caso(String categoria, String texto, TipoPedido tipoPedido, Integer mesa, List<ItemEsperado> items) {
    }

    public record Conjunto(List<ItemMenu> menu, List<Caso> casos) {
    }

    public record DetalleCaso(int numero, String categoria, String texto, boolean exacto, String esperado, String obtenido,
                              List<String> advertencias, long milisegundos, String error) {
    }

    public record Resultado(String motor, int casos, int pedidosExactos, double porcentajeExactos,
                            double precision, double recall, double f1,
                            double porcentajeMesaCorrecta, double porcentajeTipoCorrecto,
                            double porcentajeNotasCorrectas, double latenciaPromedioMs, long latenciaMaximaMs,
                            int errores, Map<String, String> exactosPorCategoria, List<DetalleCaso> detalle) {
    }

    /** Lee el conjunto de prueba desde el JSON de recursos (ia/casos-prueba.json). */
    @SuppressWarnings("unchecked")
    public static Conjunto leerConjunto(String json) {
        Map<String, Object> raiz = JsonSimple.leerObjeto(json);
        List<ItemMenu> menu = new ArrayList<>();
        for (Object o : (List<Object>) raiz.get("menu")) {
            Map<String, Object> m = (Map<String, Object>) o;
            menu.add(new ItemMenu(((Number) m.get("id")).longValue(), (String) m.get("nombre")));
        }
        List<Caso> casos = new ArrayList<>();
        for (Object o : (List<Object>) raiz.get("casos")) {
            Map<String, Object> c = (Map<String, Object>) o;
            List<ItemEsperado> items = new ArrayList<>();
            for (Object x : (List<Object>) c.get("items")) {
                Map<String, Object> it = (Map<String, Object>) x;
                items.add(new ItemEsperado((String) it.get("producto"), ((Number) it.get("cantidad")).intValue(),
                        (String) it.get("notas")));
            }
            Object mesa = c.get("mesa");
            casos.add(new Caso((String) c.get("categoria"), (String) c.get("texto"), TipoPedido.valueOf((String) c.get("tipoPedido")),
                    mesa == null ? null : ((Number) mesa).intValue(), items));
        }
        return new Conjunto(menu, casos);
    }

    /**
     * Pasa todas las frases por el intérprete.
     * pausaMs: espera entre frases, para no superar el límite de solicitudes por minuto de la API gratuita.
     */
    public static Resultado evaluar(InterpretePedido interprete, Conjunto conjunto, long pausaMs) {
        int exactos = 0, mesaOk = 0, tipoOk = 0, errores = 0;
        int unidadesCorrectas = 0, unidadesPropuestas = 0, unidadesEsperadas = 0;
        int notasEsperadas = 0, notasOk = 0;
        long sumaMs = 0, maxMs = 0;
        int medidos = 0;
        List<DetalleCaso> detalle = new ArrayList<>();
        Map<String, int[]> categorias = new LinkedHashMap<>();  // categoría -> {exactos, total}

        int n = 0;
        for (Caso caso : conjunto.casos()) {
            n++;
            int[] cat = categorias.computeIfAbsent(caso.categoria(), k -> new int[2]);
            cat[1]++;
            Map<String, Integer> esperado = totales(caso.items());
            int esperadasCaso = esperado.values().stream().mapToInt(Integer::intValue).sum();
            unidadesEsperadas += esperadasCaso;
            for (ItemEsperado it : caso.items()) {
                if (it.notas() != null) {
                    notasEsperadas++;
                }
            }
            Interpretacion r;
            try {
                if (n > 1 && pausaMs > 0) {
                    Thread.sleep(pausaMs);
                }
                r = interprete.interpretar(caso.texto(), conjunto.menu());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Evaluación interrumpida");
            } catch (RuntimeException e) {
                errores++;
                detalle.add(new DetalleCaso(n, caso.categoria(), caso.texto(), false, describir(caso.tipoPedido(), caso.mesa(), esperado),
                        null, List.of(), 0, e.getMessage()));
                continue;
            }
            medidos++;
            sumaMs += r.milisegundos();
            maxMs = Math.max(maxMs, r.milisegundos());

            Map<String, Integer> obtenido = new TreeMap<>();
            for (ItemInterpretado it : r.items()) {
                obtenido.merge(Texto.normalizar(it.productoNombre()), it.cantidad(), Integer::sum);
            }
            int propuestas = obtenido.values().stream().mapToInt(Integer::intValue).sum();
            int correctas = 0;
            for (Map.Entry<String, Integer> e : esperado.entrySet()) {
                correctas += Math.min(e.getValue(), obtenido.getOrDefault(e.getKey(), 0));
            }
            unidadesPropuestas += propuestas;
            unidadesCorrectas += correctas;

            boolean mesaBien = Objects.equals(caso.mesa(), r.mesaNumero());
            boolean tipoBien = caso.tipoPedido() == r.tipoPedido();
            if (mesaBien) {
                mesaOk++;
            }
            if (tipoBien) {
                tipoOk++;
            }
            for (ItemEsperado it : caso.items()) {
                if (it.notas() != null && notaCapturada(it, r.items())) {
                    notasOk++;
                }
            }
            boolean exacto = mesaBien && tipoBien && esperado.equals(obtenido);
            if (exacto) {
                exactos++;
                cat[0]++;
            }
            detalle.add(new DetalleCaso(n, caso.categoria(), caso.texto(), exacto, describir(caso.tipoPedido(), caso.mesa(), esperado),
                    describir(r.tipoPedido(), r.mesaNumero(), obtenido) + notasDe(r.items()),
                    r.advertencias(), r.milisegundos(), null));
        }
        int total = conjunto.casos().size();
        double precision = unidadesPropuestas == 0 ? 0 : (double) unidadesCorrectas / unidadesPropuestas;
        double recall = unidadesEsperadas == 0 ? 0 : (double) unidadesCorrectas / unidadesEsperadas;
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        Map<String, String> porCategoria = new LinkedHashMap<>();
        categorias.forEach((k, v) -> porCategoria.put(k, v[0] + "/" + v[1] + " (" + pct(v[0], v[1]) + "%)"));
        return new Resultado(interprete.nombre(), total, exactos, pct(exactos, total),
                redondear(precision), redondear(recall), redondear(f1),
                pct(mesaOk, total), pct(tipoOk, total), pct(notasOk, notasEsperadas),
                medidos == 0 ? 0 : Math.round(sumaMs * 10.0 / medidos) / 10.0, maxMs, errores, porCategoria, detalle);
    }

    /** Suma cantidades por producto (dos líneas de 1 hamburguesa = 2 hamburguesas). */
    private static Map<String, Integer> totales(List<ItemEsperado> items) {
        Map<String, Integer> m = new TreeMap<>();
        for (ItemEsperado it : items) {
            m.merge(Texto.normalizar(it.producto()), it.cantidad(), Integer::sum);
        }
        return m;
    }

    /** La nota cuenta como capturada si alguna línea del mismo producto contiene sus palabras clave. */
    static boolean notaCapturada(ItemEsperado esperado, List<ItemInterpretado> obtenidos) {
        Set<String> claves = new HashSet<>(Arrays.asList(Texto.normalizar(esperado.notas()).split(" ")));
        claves.removeAll(VACIAS_NOTA);
        String producto = Texto.normalizar(esperado.producto());
        for (ItemInterpretado it : obtenidos) {
            if (it.notas() == null || !Texto.normalizar(it.productoNombre()).equals(producto)) {
                continue;
            }
            Set<String> palabras = new HashSet<>(Arrays.asList(Texto.normalizar(it.notas()).split(" ")));
            if (palabras.containsAll(claves)) {
                return true;
            }
        }
        return false;
    }

    private static String describir(TipoPedido tipo, Integer mesa, Map<String, Integer> productos) {
        String lugar = tipo == TipoPedido.PARA_LLEVAR ? "para llevar" : "mesa " + (mesa == null ? "?" : mesa);
        StringJoiner j = new StringJoiner(", ");
        productos.forEach((p, c) -> j.add(c + " " + p));
        return lugar + " | " + (j.length() == 0 ? "(nada)" : j.toString());
    }

    private static String notasDe(List<ItemInterpretado> items) {
        StringJoiner j = new StringJoiner("; ", " | notas: ", "");
        j.setEmptyValue("");
        for (ItemInterpretado it : items) {
            if (it.notas() != null) {
                j.add(Texto.normalizar(it.productoNombre()) + " " + it.notas());
            }
        }
        return j.toString();
    }

    private static double pct(int a, int b) {
        return b == 0 ? 0 : Math.round(a * 1000.0 / b) / 10.0;
    }

    private static double redondear(double x) {
        return Math.round(x * 1000.0) / 1000.0;
    }
}
