package com.poli.restaurante.controller.admin;

import com.poli.restaurante.dto.MesaRequest;
import com.poli.restaurante.dto.MesaResponse;
import com.poli.restaurante.service.AdminFacade;
import com.poli.restaurante.seguridad.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-13: el administrador gestiona las mesas del salón. */
@RestController
@RequestMapping("/api/admin/mesas")
@RequiredArgsConstructor
public class AdminMesaController {

    private final AdminFacade facade;

    @GetMapping
    public List<MesaResponse> listar(@UsuarioActual Long adminId) {
        return facade.mesas(adminId);
    }

    @PostMapping
    public ResponseEntity<MesaResponse> crear(@UsuarioActual Long adminId,
                                              @Valid @RequestBody MesaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.crearMesa(adminId, req));
    }

    @PutMapping("/{id}")
    public MesaResponse cambiarNumero(@UsuarioActual Long adminId,
                                      @PathVariable Long id, @Valid @RequestBody MesaRequest req) {
        return facade.cambiarNumeroMesa(adminId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@UsuarioActual Long adminId,
                                         @PathVariable Long id) {
        facade.eliminarMesa(adminId, id);
        return ResponseEntity.noContent().build();
    }
}
