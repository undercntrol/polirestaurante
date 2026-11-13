package com.poli.restaurante.exception;

/** El servicio de IA externo falló (sin conexión, clave no válida, límite de uso alcanzado...). */
public class ServicioIaException extends RuntimeException {

    public ServicioIaException(String mensaje) {
        super(mensaje);
    }
}
