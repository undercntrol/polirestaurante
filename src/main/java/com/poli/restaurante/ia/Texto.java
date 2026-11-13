package com.poli.restaurante.ia;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Utilidades de texto en español: minúsculas sin tildes y números escritos en palabras. */
public final class Texto {

    private Texto() {
    }

    private static final Map<String, Integer> NUMEROS = Map.ofEntries(
            Map.entry("un", 1), Map.entry("una", 1), Map.entry("uno", 1),
            Map.entry("dos", 2), Map.entry("tres", 3), Map.entry("cuatro", 4), Map.entry("cinco", 5),
            Map.entry("seis", 6), Map.entry("siete", 7), Map.entry("ocho", 8), Map.entry("nueve", 9),
            Map.entry("diez", 10), Map.entry("once", 11), Map.entry("doce", 12), Map.entry("trece", 13),
            Map.entry("catorce", 14), Map.entry("quince", 15), Map.entry("veinte", 20));

    /** "Hamburguésa, SIN cebolla!" -> "hamburguesa , sin cebolla" */
    public static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[,;:.!?¡¿()+]", " , ")
                .replaceAll("[^a-z0-9ñ, ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Número escrito con cifras o en palabras; null si no es un número. */
    public static Integer numero(String palabra) {
        if (palabra.matches("\\d{1,3}")) {
            return Integer.parseInt(palabra);
        }
        return NUMEROS.get(palabra);
    }
}
