package com.poli.restaurante.exception;

public class CuentaInactivaException extends RuntimeException {

    public CuentaInactivaException() {
        super("La cuenta está inactiva. Consulta con el administrador");
    }
}
