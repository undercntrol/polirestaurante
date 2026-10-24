package com.poli.restaurante.service;

import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.Administrador;
import com.poli.restaurante.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica que quien llama a un endpoint de administración sea un administrador activo.
 * Mientras no haya token de sesión, el id llega en la cabecera X-Administrador-Id.
 */
@Service
@RequiredArgsConstructor
public class AccesoService {

    public static final String CABECERA_ADMIN = "X-Administrador-Id";

    private final AdministradorRepository administradorRepository;

    @Transactional(readOnly = true)
    public Administrador exigirAdministrador(Long administradorId) {
        Administrador admin = administradorRepository.findById(administradorId)
                .orElseThrow(() -> new NoEncontradoException("Administrador no encontrado: " + administradorId));
        if (!admin.isActivo()) {
            throw new ReglaNegocioException("La cuenta del administrador está inactiva");
        }
        return admin;
    }
}
