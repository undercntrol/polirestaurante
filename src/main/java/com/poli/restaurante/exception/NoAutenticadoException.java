package com.poli.restaurante.exception;

/** Sin sesión o con una sesión vencida: responde 401 y el frontend vuelve al inicio de sesión. */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
