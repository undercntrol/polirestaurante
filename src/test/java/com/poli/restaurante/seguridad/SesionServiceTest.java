package com.poli.restaurante.seguridad;

import com.poli.restaurante.exception.NoAutenticadoException;
import com.poli.restaurante.exception.SinPermisoException;
import com.poli.restaurante.model.Cocina;
import com.poli.restaurante.model.Mesero;
import com.poli.restaurante.model.Rol;
import com.poli.restaurante.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas sin base de datos ni servidor: sesiones, vencimiento y permisos por rol. */
class SesionServiceTest {

    /** Reloj que se puede adelantar a mano. */
    static class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-11-18T12:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojManual reloj = new RelojManual();
    private final SesionService sesiones = new SesionService(Duration.ofHours(8), reloj);

    private static <T extends Usuario> T usuario(T u, long id, String nombre) {
        u.setId(id);
        u.setNombre(nombre);
        return u;
    }

    @Test
    void iniciarDevuelveUnTokenUnicoConElRolDelUsuario() {
        Sesion a = sesiones.iniciar(usuario(new Mesero(), 1, "Mesero Demo"));
        Sesion b = sesiones.iniciar(usuario(new Mesero(), 1, "Mesero Demo"));

        assertEquals(Rol.MESERO, a.rol());
        assertEquals(1L, a.usuarioId());
        assertNotEquals(a.token(), b.token());
        assertTrue(a.token().length() >= 43, "256 bits en Base64 son 43 caracteres");
        assertEquals(a, sesiones.buscar(a.token()).orElseThrow());
    }

    @Test
    void tokenInventadoOVacioNoTieneSesion() {
        assertTrue(sesiones.buscar("inventado").isEmpty());
        assertTrue(sesiones.buscar("").isEmpty());
        assertTrue(sesiones.buscar(null).isEmpty());
    }

    @Test
    void laSesionVenceALasOchoHoras() {
        Sesion s = sesiones.iniciar(usuario(new Cocina(), 1, "Cocina Demo"));
        reloj.ahora = reloj.ahora.plus(Duration.ofHours(8)).minusSeconds(1);
        assertTrue(sesiones.buscar(s.token()).isPresent());
        reloj.ahora = reloj.ahora.plusSeconds(1);
        assertTrue(sesiones.buscar(s.token()).isEmpty());
        assertEquals(0, sesiones.abiertas(), "la sesión vencida se borra");
    }

    @Test
    void cerrarSesionInvalidaElToken() {
        Sesion s = sesiones.iniciar(usuario(new Mesero(), 1, "Mesero Demo"));
        sesiones.cerrar(s.token());
        assertTrue(sesiones.buscar(s.token()).isEmpty());
    }

    @Test
    void desactivarUnaCuentaCierraSoloSusSesiones() {
        Sesion mesero1 = sesiones.iniciar(usuario(new Mesero(), 1, "Mesero 1"));
        Sesion mesero2 = sesiones.iniciar(usuario(new Mesero(), 2, "Mesero 2"));
        Sesion cocina1 = sesiones.iniciar(usuario(new Cocina(), 1, "Cocina 1")); // mismo id, otra tabla

        sesiones.cerrarSesionesDe(Rol.MESERO, 1L);

        assertTrue(sesiones.buscar(mesero1.token()).isEmpty());
        assertTrue(sesiones.buscar(mesero2.token()).isPresent());
        assertTrue(sesiones.buscar(cocina1.token()).isPresent());
    }

    // ---------- Interceptor ----------

    private final AutenticacionInterceptor interceptor = new AutenticacionInterceptor(sesiones);

    private MockHttpServletRequest peticion(String metodo, String ruta, String token) {
        MockHttpServletRequest r = new MockHttpServletRequest(metodo, ruta);
        if (token != null) {
            r.addHeader("Authorization", "Bearer " + token);
        }
        return r;
    }

    @Test
    void sinTokenResponde401() {
        MockHttpServletRequest r = peticion("GET", "/api/cocina/pedidos", null);
        assertThrows(NoAutenticadoException.class,
                () -> interceptor.preHandle(r, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void unMeseroNoPuedeUsarLasRutasDeCocinaNiDeAdministracion() {
        String token = sesiones.iniciar(usuario(new Mesero(), 1, "Mesero Demo")).token();
        assertThrows(SinPermisoException.class, () -> interceptor.preHandle(
                peticion("PUT", "/api/cocina/pedidos/1/cancelar", token), new MockHttpServletResponse(), new Object()));
        assertThrows(SinPermisoException.class, () -> interceptor.preHandle(
                peticion("GET", "/api/admin/usuarios", token), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void conTokenValidoDejaLaSesionEnLaPeticion() {
        Sesion s = sesiones.iniciar(usuario(new Cocina(), 1, "Cocina Demo"));
        MockHttpServletRequest r = peticion("PUT", "/api/cocina/pedidos/1/cancelar", s.token());

        assertTrue(interceptor.preHandle(r, new MockHttpServletResponse(), new Object()));
        assertEquals(s, r.getAttribute(Sesion.ATRIBUTO));
    }

    @Test
    void lasConsultasDeApoyoSirvenParaCualquierRol() {
        for (Rol rol : Rol.values()) {
            assertTrue(AutenticacionInterceptor.rolesPermitidos("/api/menu").contains(rol));
        }
        assertEquals(java.util.Set.of(Rol.MESERO), AutenticacionInterceptor.rolesPermitidos("/api/pedidos"));
        assertEquals(java.util.Set.of(Rol.MESERO), AutenticacionInterceptor.rolesPermitidos("/api/pedidos/5/cancelar"));
        assertFalse(AutenticacionInterceptor.rolesPermitidos("/api/administrativo").equals(java.util.Set.of(Rol.ADMINISTRADOR)),
                "solo /api/admin y sus subrutas son de administración");
    }
}
