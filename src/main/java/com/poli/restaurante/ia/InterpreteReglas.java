package com.poli.restaurante.ia;

import com.poli.restaurante.model.TipoPedido;

import java.util.*;

/**
 * Intérprete sin IA, basado en reglas fijas. Sirve como línea base para comparar con el modelo de lenguaje.
 *
 * Reglas:
 * - "mesa" seguido de un número (en cifras o en palabras) es el número de mesa.
 * - "para llevar" o "domicilio" marca el pedido como PARA_LLEVAR.
 * - Un número seguido de un producto del menú es la cantidad; si no hay número, la cantidad es 1.
 * - Un producto se reconoce por las palabras de su nombre, aceptando plurales y diminutivos simples
 *   ("hamburguesas", "gaseositas").
 * - Lo que sigue a "sin", "con", "extra", "bien"... después de un producto son sus notas.
 * - Un número seguido de algo que no está en el menú genera una advertencia.
 */
public class InterpreteReglas implements InterpretePedido {

    private static final Set<String> VACIAS = Set.of(
            "de", "del", "la", "el", "los", "las", "a", "al", "y", "por", "favor", "porfa", "me", "nos",
            "regalas", "regala", "regalan", "quiero", "queremos", "quieren", "para", "dame", "danos", "traeme",
            "traenos", "en", "es", "pedido", "unos", "unas", "tambien", "mas", "ademas", "porfavor", "ah", "con");

    private static final Set<String> INICIO_NOTA = Set.of(
            "sin", "con", "extra", "bien", "poco", "poca", "doble", "mucho", "mucha", "termino", "aparte", "tibia",
            "tibio", "fria", "frio", "caliente", "natural", "light");

    @Override
    public String nombre() {
        return "REGLAS";
    }

    @Override
    public boolean disponible() {
        return true;
    }

    @Override
    public Interpretacion interpretar(String texto, List<ItemMenu> menu) {
        long inicio = System.nanoTime();
        String[] t = Texto.normalizar(texto).split(" ");
        boolean[] usado = new boolean[t.length];
        List<String> advertencias = new ArrayList<>();

        // 1. Tipo de pedido y mesa
        TipoPedido tipo = TipoPedido.EN_SITIO;
        Integer mesa = null;
        for (int i = 0; i < t.length; i++) {
            if (t[i].equals("llevar") || t[i].equals("domicilio")) {
                tipo = TipoPedido.PARA_LLEVAR;
                usado[i] = true;
            }
            if (t[i].equals("mesa") && i + 1 < t.length && Texto.numero(t[i + 1]) != null) {
                mesa = Texto.numero(t[i + 1]);
                usado[i] = true;
                usado[i + 1] = true;
            }
        }
        if (tipo == TipoPedido.PARA_LLEVAR) {
            mesa = null;
        }

        // 2. Productos, cantidades y notas
        Indice indice = new Indice(menu);
        List<ItemInterpretado> items = new ArrayList<>();
        ItemMenu actual = null;
        int cantidadActual = 1;
        StringBuilder notas = new StringBuilder();
        boolean enNota = false;
        Integer cantidadPendiente = null;

        int i = 0;
        while (i < t.length) {
            String p = t[i];
            if (usado[i] || p.isEmpty()) {
                i++;
                continue;
            }
            if (p.equals(",")) {
                enNota = false;
                i++;
                continue;
            }
            Coincidencia producto = indice.buscar(t, i, usado);
            if (producto != null && !(enNota && esNotaDeIngrediente(t, i))) {
                agregar(items, actual, cantidadActual, notas);
                actual = producto.item();
                cantidadActual = cantidadPendiente != null ? cantidadPendiente : 1;
                cantidadPendiente = null;
                notas.setLength(0);
                enNota = false;
                i += producto.palabras();
                continue;
            }
            Integer n = Texto.numero(p);
            if (n != null) {
                // Dentro de una nota, un número solo cuenta como cantidad si lo sigue un producto
                if (enNota && (i + 1 >= t.length || indice.buscar(t, i + 1, usado) == null)) {
                    notas.append(' ').append(p);
                } else {
                    cantidadPendiente = n;
                    enNota = false;
                }
                i++;
                continue;
            }
            // "dos jugos, uno sin azúcar": separa una parte de la línea anterior con su propia nota
            if (actual != null && INICIO_NOTA.contains(p) && cantidadPendiente != null
                    && cantidadPendiente < cantidadActual) {
                agregar(items, actual, cantidadActual - cantidadPendiente, notas);
                cantidadActual = cantidadPendiente;
                cantidadPendiente = null;
                notas.setLength(0);
                enNota = true;
                notas.append(' ').append(p);
                i++;
                continue;
            }
            // "otra" / "otro" sin producto después: una unidad más del último producto
            if ((p.equals("otra") || p.equals("otro")) && actual != null
                    && (i + 1 >= t.length || indice.buscar(t, i + 1, usado) == null)) {
                agregar(items, actual, cantidadActual, notas);
                cantidadActual = 1;
                notas.setLength(0);
                enNota = false;
                i++;
                continue;
            }
            if (actual != null && INICIO_NOTA.contains(p) && cantidadPendiente == null) {
                enNota = true;
                notas.append(' ').append(p);
                i++;
                continue;
            }
            if (enNota && actual != null) {
                if (p.equals("y") && i + 1 < t.length && !INICIO_NOTA.contains(t[i + 1])) {
                    enNota = false;     // "y" que separa productos, no parte de la nota
                } else {
                    notas.append(' ').append(p);
                }
                i++;
                continue;
            }
            if (cantidadPendiente != null && !VACIAS.contains(p)) {
                StringBuilder desconocido = new StringBuilder(p);
                if (i + 1 < t.length && !VACIAS.contains(t[i + 1]) && !t[i + 1].equals(",")
                        && Texto.numero(t[i + 1]) == null && indice.buscar(t, i + 1, usado) == null) {
                    desconocido.append(' ').append(t[i + 1]);
                    i++;
                }
                advertencias.add("No está en el menú: " + desconocido);
                cantidadPendiente = null;
            }
            i++;
        }
        agregar(items, actual, cantidadActual, notas);

        if (items.isEmpty()) {
            advertencias.add("No se reconoció ningún producto del menú");
        }
        if (tipo == TipoPedido.EN_SITIO && mesa == null) {
            advertencias.add("No se indicó la mesa");
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        return new Interpretacion(nombre(), tipo, mesa, items, advertencias, ms);
    }

    /** "con papas" después de "hamburguesa" se toma como nota solo si la palabra no inicia otro producto con cantidad. */
    private boolean esNotaDeIngrediente(String[] t, int i) {
        return i > 0 && (t[i - 1].equals("sin") || t[i - 1].equals("extra"));
    }

    private void agregar(List<ItemInterpretado> items, ItemMenu item, int cantidad, StringBuilder notas) {
        if (item == null) {
            return;
        }
        String n = notas.toString().trim();
        items.add(new ItemInterpretado(item.id(), item.nombre(), Math.max(1, cantidad), n.isEmpty() ? null : n));
    }

    /** Coincidencia de un producto que empieza en una posición y ocupa cierto número de palabras. */
    record Coincidencia(ItemMenu item, int palabras) {
    }

    /** Palabras clave de cada producto del menú para reconocerlos en el texto. */
    static final class Indice {

        private final Map<ItemMenu, List<String>> claves = new LinkedHashMap<>();
        private final Map<String, Integer> frecuencia = new HashMap<>();

        Indice(List<ItemMenu> menu) {
            for (ItemMenu m : menu) {
                List<String> palabras = new ArrayList<>();
                for (String w : Texto.normalizar(m.nombre()).split(" ")) {
                    if (!VACIAS.contains(w) && !w.equals(",") && w.length() > 2) {
                        palabras.add(w);
                    }
                }
                claves.put(m, palabras);
                for (String w : new HashSet<>(palabras)) {
                    frecuencia.merge(w, 1, Integer::sum);
                }
            }
        }

        /**
         * Busca un producto que empiece en la palabra i. Vale la primera palabra del nombre
         * ("hamburguesa") o una palabra que solo tenga ese producto ("clasica").
         * Si varios coinciden, gana el que tenga más palabras de su nombre cerca.
         */
        Coincidencia buscar(String[] t, int i, boolean[] usado) {
            if (i >= t.length || usado[i]) {
                return null;
            }
            Coincidencia mejor = null;
            int mejorPuntaje = 0;
            for (Map.Entry<ItemMenu, List<String>> e : claves.entrySet()) {
                List<String> palabras = e.getValue();
                if (palabras.isEmpty()) {
                    continue;
                }
                boolean empieza = false;
                for (int k = 0; k < palabras.size(); k++) {
                    String w = palabras.get(k);
                    if (coincide(t[i], w) && (k == 0 || frecuencia.get(w) == 1)) {
                        empieza = true;
                        break;
                    }
                }
                if (!empieza) {
                    continue;
                }
                int puntaje = 0;
                int ultima = i;
                for (int j = i; j < Math.min(t.length, i + 5); j++) {
                    if (j > i && (t[j].equals(",") || Texto.numero(t[j]) != null)) {
                        break;
                    }
                    for (String w : palabras) {
                        if (coincide(t[j], w)) {
                            puntaje++;
                            ultima = j;
                            break;
                        }
                    }
                }
                if (puntaje > mejorPuntaje) {
                    mejorPuntaje = puntaje;
                    mejor = new Coincidencia(e.getKey(), ultima - i + 1);
                }
            }
            return mejor;
        }

        /** Igual, plural ("jugos", "papas") o con el mismo inicio de 5 letras ("gaseositas", "hamburguesas"). */
        static boolean coincide(String palabra, String clave) {
            if (palabra.equals(clave)) {
                return true;
            }
            if (palabra.endsWith("es") && palabra.substring(0, palabra.length() - 2).equals(clave)) {
                return true;
            }
            if (palabra.endsWith("s") && palabra.substring(0, palabra.length() - 1).equals(clave)) {
                return true;
            }
            return clave.length() >= 5 && palabra.length() >= 5 && palabra.startsWith(clave.substring(0, 5));
        }
    }
}
