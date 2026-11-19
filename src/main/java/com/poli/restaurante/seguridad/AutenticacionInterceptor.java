package com.poli.restaurante.seguridad;

import com.poli.restaurante.exception.NoAutenticadoException;
import com.poli.restaurante.exception.SinPermisoException;
import com.poli.restaurante.model.Rol;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.EnumSet;
import java.util.Set;

/**
 * Se ejecuta antes de cada endpoint de /api (menos el inicio de sesión):
 * 1. exige la cabecera {@code Authorization: Bearer <token>} con una sesión vigente (si no, 401);
 * 2. revisa que el rol de la sesión pueda usar esa ruta (si no, 403);
 * 3. deja la sesión en la petición para que los controladores sepan quién llama.
 */
@Component
@RequiredArgsConstructor
public class AutenticacionInterceptor implements HandlerInterceptor {

    private static final String PREFIJO = "Bearer ";

    private final SesionService sesiones;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true; // el navegador pregunta por CORS antes de enviar la cabecera Authorization
        }
        Sesion sesion = sesiones.buscar(token(request))
                .orElseThrow(() -> new NoAutenticadoException("Inicia sesión para continuar"));

        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (!rolesPermitidos(ruta).contains(sesion.rol())) {
            throw new SinPermisoException("Un usuario con rol " + sesion.rol() + " no puede usar esta función");
        }
        request.setAttribute(Sesion.ATRIBUTO, sesion);
        return true;
    }

    /** Qué roles pueden usar cada grupo de rutas. */
    static Set<Rol> rolesPermitidos(String ruta) {
        if (bajo(ruta, "/api/admin")) {
            return EnumSet.of(Rol.ADMINISTRADOR);
        }
        if (bajo(ruta, "/api/cocina")) {
            return EnumSet.of(Rol.COCINA);
        }
        if (bajo(ruta, "/api/pedidos")) {
            return EnumSet.of(Rol.MESERO);
        }
        if (bajo(ruta, "/api/ia")) {
            return EnumSet.of(Rol.MESERO, Rol.ADMINISTRADOR);
        }
        // Consultas de apoyo (menú, mesas, métodos de pago) y /api/auth/*: cualquier usuario con sesión
        return EnumSet.allOf(Rol.class);
    }

    static String token(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            return null;
        }
        return cabecera.substring(PREFIJO.length()).trim();
    }

    private static boolean bajo(String ruta, String prefijo) {
        return ruta.equals(prefijo) || ruta.startsWith(prefijo + "/");
    }
}
