package com.poli.restaurante.dto;

import com.poli.restaurante.model.Rol;
import com.poli.restaurante.seguridad.Sesion;

import java.time.Instant;

/**
 * RF-11: respuesta del inicio de sesión. El frontend guarda el token y lo envía en la cabecera
 * {@code Authorization: Bearer <token>} de cada petición. Nunca incluye la contraseña ni su hash.
 */
public record LoginResponse(String token, Instant expira, Long id, Rol rol, String nombre) {

    public static LoginResponse de(Sesion s) {
        return new LoginResponse(s.token(), s.expira(), s.usuarioId(), s.rol(), s.nombre());
    }
}
