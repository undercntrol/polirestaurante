package com.poli.restaurante.ia;

import com.poli.restaurante.model.TipoPedido;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Intérprete con un modelo de lenguaje (Google Gemini) a través de la API REST generateContent.
 *
 * Al modelo se le envía el menú (id y nombre de cada producto) y se le pide responder solo con un JSON
 * con el tipo de pedido, la mesa y las líneas del pedido. Luego se valida esa respuesta: se descartan
 * productos que no estén en el menú y cantidades no válidas, para que el modelo nunca pueda
 * inventar un producto ni saltarse las reglas del sistema.
 */
public class InterpreteGemini implements InterpretePedido {

    private final String apiKey;
    private final String modelo;
    private final String urlBase;
    private final HttpClient http;
    private final Duration tiempoMaximo;

    public InterpreteGemini(String apiKey, String modelo, String urlBase, Duration tiempoMaximo) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.tiempoMaximo = tiempoMaximo;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String nombre() {
        return "GEMINI";
    }

    @Override
    public boolean disponible() {
        return !apiKey.isEmpty();
    }

    public String modelo() {
        return modelo;
    }

    @Override
    public Interpretacion interpretar(String texto, List<ItemMenu> menu) {
        if (!disponible()) {
            throw new IllegalStateException("Falta configurar GEMINI_API_KEY en el archivo .env");
        }
        long inicio = System.nanoTime();
        String respuesta = llamarApi(instrucciones(menu), texto);
        Interpretacion i = convertir(respuesta, menu);
        return i.conTiempo((System.nanoTime() - inicio) / 1_000_000);
    }

    /** Instrucciones del sistema: el rol, el menú y el formato exacto de la respuesta. */
    static String instrucciones(List<ItemMenu> menu) {
        StringBuilder m = new StringBuilder();
        for (ItemMenu item : menu) {
            m.append("- id ").append(item.id()).append(": ").append(item.nombre()).append('\n');
        }
        return """
                Eres el asistente de toma de pedidos de un restaurante de comida rápida en Colombia.
                Recibes lo que dicta un mesero y lo conviertes en un pedido estructurado.

                MENÚ DISPONIBLE (solo puedes usar estos productos y sus id):
                %s
                Responde ÚNICAMENTE con un objeto JSON con esta forma exacta:
                {"tipoPedido": "EN_SITIO" o "PARA_LLEVAR",
                 "mesaNumero": número entero o null,
                 "items": [{"productoId": número, "cantidad": número entero >= 1, "notas": texto o null}],
                 "advertencias": [texto]}

                Reglas:
                1. Si dicen "para llevar" o "domicilio", tipoPedido es PARA_LLEVAR y mesaNumero es null.
                   Si no, tipoPedido es EN_SITIO y mesaNumero es el número de mesa que mencionen (null si no la dicen).
                2. Relaciona cada cosa pedida con el producto del menú que corresponda, aunque la escriban con
                   errores de ortografía, en plural, en diminutivo o con nombres comunes (por ejemplo, una marca de
                   gaseosa corresponde a "Gaseosa"). Las cantidades pueden venir en palabras ("dos", "un par").
                3. notas: modificaciones del cliente en minúscula, como "sin cebolla" o "con extra queso"; null si no hay.
                   Si el mismo producto se pide con notas distintas, usa una línea por cada variante.
                4. Si el mesero corrige algo ("no, mejor dos"), usa la versión final.
                5. Si piden algo que NO está en el menú, NO lo incluyas en items: escribe en advertencias
                   "No está en el menú: <lo que pidieron>".
                6. No inventes productos, cantidades ni mesas.
                """.formatted(m);
    }

    private String llamarApi(String sistema, String texto) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("system_instruction", Map.of("parts", List.of(Map.of("text", sistema))));
        cuerpo.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", texto)))));
        cuerpo.put("generationConfig", Map.of("temperature", 0, "responseMimeType", "application/json"));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/v1beta/models/" + modelo + ":generateContent"))
                .timeout(tiempoMaximo)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JsonSimple.escribir(cuerpo)))
                .build();
        HttpResponse<String> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo conectar con Gemini: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la llamada a Gemini", e);
        }
        if (res.statusCode() == 429) {
            throw new IllegalStateException("Gemini respondió 429: se alcanzó el límite de uso gratuito. Espera un minuto.");
        }
        if (res.statusCode() != 200) {
            throw new IllegalStateException("Gemini respondió " + res.statusCode() + ": " + mensajeError(res.body()));
        }
        return textoDeRespuesta(res.body());
    }

    /** Une el texto de candidates[0].content.parts[*].text */
    @SuppressWarnings("unchecked")
    static String textoDeRespuesta(String cuerpo) {
        Map<String, Object> r = JsonSimple.leerObjeto(cuerpo);
        List<Object> candidatos = (List<Object>) r.get("candidates");
        if (candidatos == null || candidatos.isEmpty()) {
            throw new IllegalStateException("Gemini no devolvió ninguna respuesta");
        }
        Map<String, Object> contenido = (Map<String, Object>) ((Map<String, Object>) candidatos.get(0)).get("content");
        if (contenido == null || contenido.get("parts") == null) {
            throw new IllegalStateException("La respuesta de Gemini llegó vacía");
        }
        StringBuilder b = new StringBuilder();
        for (Object parte : (List<Object>) contenido.get("parts")) {
            Object t = ((Map<String, Object>) parte).get("text");
            if (t != null) {
                b.append(t);
            }
        }
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private static String mensajeError(String cuerpo) {
        try {
            Map<String, Object> e = (Map<String, Object>) JsonSimple.leerObjeto(cuerpo).get("error");
            return String.valueOf(e.get("message"));
        } catch (RuntimeException ex) {
            return cuerpo.length() > 300 ? cuerpo.substring(0, 300) : cuerpo;
        }
    }

    /** Valida el JSON del modelo contra el menú y lo convierte en una Interpretacion. */
    @SuppressWarnings("unchecked")
    Interpretacion convertir(String textoModelo, List<ItemMenu> menu) {
        String json = textoModelo.trim();
        if (json.startsWith("```")) {   // por si el modelo envuelve el JSON en un bloque de código
            json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "");
        }
        Map<String, Object> r;
        try {
            r = JsonSimple.leerObjeto(json);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Gemini no respondió un JSON válido: " + e.getMessage());
        }
        Map<Long, ItemMenu> porId = new HashMap<>();
        for (ItemMenu m : menu) {
            porId.put(m.id(), m);
        }
        List<String> advertencias = new ArrayList<>();
        if (r.get("advertencias") instanceof List<?> l) {
            for (Object a : l) {
                if (a != null && !String.valueOf(a).isBlank()) {
                    advertencias.add(String.valueOf(a));
                }
            }
        }
        TipoPedido tipo = "PARA_LLEVAR".equals(r.get("tipoPedido")) ? TipoPedido.PARA_LLEVAR : TipoPedido.EN_SITIO;
        Integer mesa = r.get("mesaNumero") instanceof Number n ? n.intValue() : null;
        if (tipo == TipoPedido.PARA_LLEVAR) {
            mesa = null;
        }
        List<ItemInterpretado> items = new ArrayList<>();
        if (r.get("items") instanceof List<?> lista) {
            for (Object o : lista) {
                if (!(o instanceof Map<?, ?> it)) {
                    continue;
                }
                Long id = it.get("productoId") instanceof Number n ? n.longValue() : null;
                ItemMenu producto = id == null ? null : porId.get(id);
                if (producto == null) {
                    advertencias.add("El modelo propuso un producto que no existe en el menú (id " + id + "); se descartó");
                    continue;
                }
                int cantidad = it.get("cantidad") instanceof Number n ? n.intValue() : 1;
                if (cantidad < 1 || cantidad > 50) {
                    advertencias.add("Cantidad no válida para " + producto.nombre() + ": " + cantidad + "; se descartó");
                    continue;
                }
                Object notas = it.get("notas");
                String textoNotas = notas == null || String.valueOf(notas).isBlank() ? null : String.valueOf(notas).trim();
                items.add(new ItemInterpretado(producto.id(), producto.nombre(), cantidad, textoNotas));
            }
        }
        if (items.isEmpty() && advertencias.isEmpty()) {
            advertencias.add("No se reconoció ningún producto del menú");
        }
        if (tipo == TipoPedido.EN_SITIO && mesa == null) {
            advertencias.add("No se indicó la mesa");
        }
        return new Interpretacion(nombre(), tipo, mesa, items, advertencias, 0);
    }
}
