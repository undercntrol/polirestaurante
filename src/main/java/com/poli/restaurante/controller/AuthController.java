package com.poli.restaurante.controller;

import com.poli.restaurante.dto.LoginRequest;
import com.poli.restaurante.dto.UsuarioResponse;
import com.poli.restaurante.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** RF-11 y HU-14: inicio de sesión. Devuelve el id y el rol que el frontend usa en las demás llamadas. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest req) {
        return UsuarioResponse.de(usuarioService.login(req.username(), req.password()));
    }
}
