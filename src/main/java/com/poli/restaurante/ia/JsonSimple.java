package com.poli.restaurante.ia;

import java.util.*;

/**
 * Lector y escritor de JSON mínimo, sin librerías externas.
 * Se usa para hablar con la API de Gemini y leer el conjunto de prueba, así este paquete
 * no depende de la versión de Jackson que traiga Spring Boot.
 * Objetos -> Map<String,Object>, arreglos -> List<Object>, números -> Long o Double.
 */
public final class JsonSimple {

    private final String s;
    private int i;

    private JsonSimple(String s) {
        this.s = s;
    }

    public static Object leer(String json) {
        JsonSimple p = new JsonSimple(json);
        p.espacios();
        Object v = p.valor();
        p.espacios();
        if (p.i != p.s.length()) {
            throw p.error("Sobra texto después del JSON");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> leerObjeto(String json) {
        Object v = leer(json);
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON");
        }
        return (Map<String, Object>) v;
    }

    private Object valor() {
        if (i >= s.length()) {
            throw error("JSON incompleto");
        }
        char c = s.charAt(i);
        return switch (c) {
            case '{' -> objeto();
            case '[' -> arreglo();
            case '"' -> cadena();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> numero();
        };
    }

    private Map<String, Object> objeto() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        espacios();
        if (s.charAt(i) == '}') {
            i++;
            return m;
        }
        while (true) {
            espacios();
            String clave = cadena();
            espacios();
            esperar(':');
            espacios();
            m.put(clave, valor());
            espacios();
            if (s.charAt(i) == ',') {
                i++;
            } else {
                esperar('}');
                return m;
            }
        }
    }

    private List<Object> arreglo() {
        List<Object> l = new ArrayList<>();
        i++;
        espacios();
        if (s.charAt(i) == ']') {
            i++;
            return l;
        }
        while (true) {
            espacios();
            l.add(valor());
            espacios();
            if (s.charAt(i) == ',') {
                i++;
            } else {
                esperar(']');
                return l;
            }
        }
    }

    private String cadena() {
        esperar('"');
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') {
                return b.toString();
            }
            if (c != '\\') {
                b.append(c);
                continue;
            }
            char e = s.charAt(i++);
            switch (e) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> {
                    b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                }
                default -> b.append(e);
            }
        }
    }

    private Object numero() {
        int ini = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        String n = s.substring(ini, i);
        if (n.isEmpty()) {
            throw error("Valor JSON no válido");
        }
        return n.matches("-?\\d+") ? (Object) Long.parseLong(n) : (Object) Double.parseDouble(n);
    }

    private Object literal(String palabra, Object valor) {
        if (!s.startsWith(palabra, i)) {
            throw error("Valor JSON no válido");
        }
        i += palabra.length();
        return valor;
    }

    private void espacios() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }

    private void esperar(char c) {
        if (i >= s.length() || s.charAt(i) != c) {
            throw error("Se esperaba '" + c + "'");
        }
        i++;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg + " en la posición " + i);
    }

    // ---------- Escritura ----------

    public static String escribir(Object v) {
        StringBuilder b = new StringBuilder();
        escribir(v, b);
        return b.toString();
    }

    private static void escribir(Object v, StringBuilder b) {
        if (v == null) {
            b.append("null");
        } else if (v instanceof String str) {
            b.append('"');
            for (char c : str.toCharArray()) {
                switch (c) {
                    case '"' -> b.append("\\\"");
                    case '\\' -> b.append("\\\\");
                    case '\n' -> b.append("\\n");
                    case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            b.append(String.format("\\u%04x", (int) c));
                        } else {
                            b.append(c);
                        }
                    }
                }
            }
            b.append('"');
        } else if (v instanceof Number || v instanceof Boolean) {
            b.append(v);
        } else if (v instanceof Map<?, ?> m) {
            b.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primero) {
                    b.append(',');
                }
                primero = false;
                escribir(String.valueOf(e.getKey()), b);
                b.append(':');
                escribir(e.getValue(), b);
            }
            b.append('}');
        } else if (v instanceof Collection<?> c) {
            b.append('[');
            boolean primero = true;
            for (Object o : c) {
                if (!primero) {
                    b.append(',');
                }
                primero = false;
                escribir(o, b);
            }
            b.append(']');
        } else {
            escribir(String.valueOf(v), b);
        }
    }
}
