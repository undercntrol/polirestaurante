package com.poli.restaurante.dto;

import com.poli.restaurante.model.Rol;
import com.poli.restaurante.model.Usuario;

/** Nunca incluye la contraseña ni su hash. */
public record UsuarioResponse(Long id, Rol rol, String nombre, String username, boolean activo) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getRol(), u.getNombre(), u.getUsername(), u.isActivo());
    }
}
