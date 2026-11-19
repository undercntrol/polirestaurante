package com.poli.restaurante.seguridad;

import com.poli.restaurante.model.Rol;

import java.time.Instant;

/**
 * Una sesión abierta con el inicio de sesión. El token es lo único que el frontend guarda y envía
 * en cada petición (cabecera Authorization: Bearer ...); el id y el rol del usuario salen de aquí,
 * nunca del cuerpo de la petición.
 */
public record Sesion(String token, Long usuarioId, Rol rol, String nombre, Instant expira) {

    /** Nombre del atributo de la petición donde el interceptor deja la sesión validada. */
    public static final String ATRIBUTO = "sesion";

    public boolean vencida(Instant ahora) {
        return !ahora.isBefore(expira);
    }
}
