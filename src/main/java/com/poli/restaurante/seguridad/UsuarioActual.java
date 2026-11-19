package com.poli.restaurante.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un parámetro {@code Long} de un controlador para recibir el id del usuario que inició sesión
 * (en la tabla de su rol). Ejemplo: {@code public List<PedidoResponse> activos(@UsuarioActual Long meseroId)}.
 * Así nadie puede actuar a nombre de otro cambiando un id en el cuerpo de la petición.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioActual {
}
