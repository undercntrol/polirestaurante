package com.poli.restaurante.controller;

import com.poli.restaurante.dto.LoginRequest;
import com.poli.restaurante.dto.LoginResponse;
import com.poli.restaurante.seguridad.Sesion;
import com.poli.restaurante.seguridad.SesionService;
import com.poli.restaurante.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RF-11 y HU-14: inicio y cierre de sesión. El inicio de sesión es el único endpoint de la API que no
 * pide token; los demás exigen la cabecera {@code Authorization: Bearer <token>}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final SesionService sesiones;

    /** Valida usuario y contraseña (BCrypt) y abre una sesión. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return LoginResponse.de(sesiones.iniciar(usuarioService.login(req.username(), req.password())));
    }

    /** Quién soy: sirve para comprobar que el token sigue vigente. */
    @GetMapping("/sesion")
    public LoginResponse sesion(Sesion sesion) {
        return LoginResponse.de(sesion);
    }

    /** Cierra la sesión: el token deja de funcionar. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Sesion sesion) {
        sesiones.cerrar(sesion.token());
        return ResponseEntity.noContent().build();
    }
}
