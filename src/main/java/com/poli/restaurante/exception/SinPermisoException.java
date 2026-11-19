package com.poli.restaurante.exception;

/** Hay sesión, pero el rol no puede usar esa función: responde 403. */
public class SinPermisoException extends RuntimeException {

    public SinPermisoException(String mensaje) {
        super(mensaje);
    }
}
