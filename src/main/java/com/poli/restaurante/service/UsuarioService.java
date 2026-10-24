package com.poli.restaurante.service;

import com.poli.restaurante.dto.UsuarioRequest;
import com.poli.restaurante.exception.CredencialesInvalidasException;
import com.poli.restaurante.exception.CuentaInactivaException;
import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.*;
import com.poli.restaurante.repository.AdministradorRepository;
import com.poli.restaurante.repository.CocinaRepository;
import com.poli.restaurante.repository.MeseroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** HU-14: cuentas de mesero, cocina y administrador (cada rol en su propia tabla). RF-11: inicio de sesión. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final MeseroRepository meseroRepository;
    private final CocinaRepository cocinaRepository;
    private final AdministradorRepository administradorRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional(readOnly = true)
    public List<Usuario> todos() {
        Sort porNombre = Sort.by("nombre");
        List<Usuario> usuarios = new ArrayList<>();
        usuarios.addAll(administradorRepository.findAll(porNombre));
        usuarios.addAll(cocinaRepository.findAll(porNombre));
        usuarios.addAll(meseroRepository.findAll(porNombre));
        return usuarios;
    }

    /** HU-14: el rol decide la tabla; la contraseña se guarda con hash BCrypt, nunca en texto plano (RNF-01). */
    @Transactional
    public Usuario crear(UsuarioRequest req) {
        String username = req.username().trim();
        if (buscarPorUsername(username).isPresent()) {
            throw new ReglaNegocioException("El usuario '" + username + "' ya existe");
        }
        Usuario u = switch (req.rol()) {
            case MESERO -> new Mesero();
            case COCINA -> new Cocina();
            case ADMINISTRADOR -> new Administrador();
        };
        u.setNombre(req.nombre().trim());
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setActivo(true);
        return guardar(u);
    }

    /** HU-14: activar o desactivar una cuenta. Un administrador no puede desactivarse a sí mismo. */
    @Transactional
    public Usuario cambiarEstado(Rol rol, Long id, boolean activo, Long adminQueLlama) {
        if (rol == Rol.ADMINISTRADOR && id.equals(adminQueLlama) && !activo) {
            throw new ReglaNegocioException("No puedes desactivar tu propia cuenta");
        }
        Usuario u = buscar(rol, id);
        u.setActivo(activo);
        return u;
    }

    /**
     * RF-11 y HU-14: valida usuario y contraseña. Si la cuenta está desactivada, se rechaza
     * indicando que está inactiva.
     */
    @Transactional(readOnly = true)
    public Usuario login(String username, String password) {
        Usuario u = buscarPorUsername(username.trim()).orElseThrow(CredencialesInvalidasException::new);
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (!u.isActivo()) {
            throw new CuentaInactivaException();
        }
        return u;
    }

    /** El nombre de usuario es único entre las tres tablas. */
    private Optional<Usuario> buscarPorUsername(String username) {
        return meseroRepository.findByUsername(username).map(Usuario.class::cast)
                .or(() -> cocinaRepository.findByUsername(username).map(Usuario.class::cast))
                .or(() -> administradorRepository.findByUsername(username).map(Usuario.class::cast));
    }

    private Usuario buscar(Rol rol, Long id) {
        Optional<? extends Usuario> u = switch (rol) {
            case MESERO -> meseroRepository.findById(id);
            case COCINA -> cocinaRepository.findById(id);
            case ADMINISTRADOR -> administradorRepository.findById(id);
        };
        return u.orElseThrow(() -> new NoEncontradoException("No existe un usuario " + rol + " con id " + id));
    }

    private Usuario guardar(Usuario u) {
        if (u instanceof Mesero m) {
            return meseroRepository.save(m);
        }
        if (u instanceof Cocina c) {
            return cocinaRepository.save(c);
        }
        return administradorRepository.save((Administrador) u);
    }
}
