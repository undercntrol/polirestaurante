package com.poli.restaurante.seguridad;

import com.poli.restaurante.model.Rol;
import com.poli.restaurante.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abre, valida y cierra sesiones (RF-11). Cada inicio de sesión recibe un token aleatorio de 256 bits
 * que vence a las pocas horas.
 *
 * Las sesiones viven en memoria: si el servidor se reinicia, todos deben volver a iniciar sesión.
 * Para un solo servidor es suficiente; con varios habría que guardarlas en la base de datos o usar JWT.
 */
@Service
public class SesionService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final Duration duracion;
    private final Clock reloj;

    @Autowired
    public SesionService(@Value("${seguridad.sesion.horas:8}") long horas) {
        this(Duration.ofHours(horas), Clock.systemUTC());
    }

    /** Para las pruebas: permite fijar la duración y el reloj. */
    SesionService(Duration duracion, Clock reloj) {
        this.duracion = duracion;
        this.reloj = reloj;
    }

    /** Abre una sesión para un usuario que ya validó su contraseña. */
    public Sesion iniciar(Usuario usuario) {
        Instant ahora = reloj.instant();
        sesiones.values().removeIf(s -> s.vencida(ahora)); // limpieza de paso
        Sesion s = new Sesion(nuevoToken(), usuario.getId(), usuario.getRol(), usuario.getNombre(),
                ahora.plus(duracion));
        sesiones.put(s.token(), s);
        return s;
    }

    /** La sesión del token, si existe y no ha vencido. */
    public Optional<Sesion> buscar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Sesion s = sesiones.get(token);
        if (s == null) {
            return Optional.empty();
        }
        if (s.vencida(reloj.instant())) {
            sesiones.remove(token);
            return Optional.empty();
        }
        return Optional.of(s);
    }

    /** Cerrar sesión. */
    public void cerrar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    /** Cuando el administrador desactiva una cuenta, sus sesiones abiertas dejan de servir de inmediato. */
    public void cerrarSesionesDe(Rol rol, Long usuarioId) {
        sesiones.values().removeIf(s -> s.rol() == rol && s.usuarioId().equals(usuarioId));
    }

    int abiertas() {
        return sesiones.size();
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
