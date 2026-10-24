package com.poli.restaurante.controller.admin;

import com.poli.restaurante.dto.EstadoUsuarioRequest;
import com.poli.restaurante.dto.UsuarioRequest;
import com.poli.restaurante.dto.UsuarioResponse;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.Rol;
import com.poli.restaurante.service.AccesoService;
import com.poli.restaurante.service.AdminFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-14: el administrador gestiona las cuentas de mesero, cocina y administración. */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final AdminFacade facade;

    @GetMapping
    public List<UsuarioResponse> listar(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId) {
        return facade.usuarios(adminId);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId,
                                                 @Valid @RequestBody UsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.crearUsuario(adminId, req));
    }

    /**
     * Activar o desactivar. Como cada rol tiene su tabla, el id se repite entre roles;
     * por eso la ruta lleva el rol. Ej.: PUT /api/admin/usuarios/mesero/2/estado
     */
    @PutMapping("/{rol}/{id}/estado")
    public UsuarioResponse cambiarEstado(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId,
                                         @PathVariable String rol, @PathVariable Long id,
                                         @Valid @RequestBody EstadoUsuarioRequest req) {
        return facade.cambiarEstadoUsuario(adminId, rolDe(rol), id, req.activo());
    }

    private Rol rolDe(String texto) {
        try {
            return Rol.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Rol no válido: " + texto + ". Usa mesero, cocina o administrador");
        }
    }
}
