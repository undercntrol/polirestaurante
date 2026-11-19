package com.poli.restaurante.controller.admin;

import com.poli.restaurante.dto.CategoriaRequest;
import com.poli.restaurante.dto.CategoriaResponse;
import com.poli.restaurante.service.AdminFacade;
import com.poli.restaurante.seguridad.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-12: el administrador crea, renombra y elimina categorías del menú. */
@RestController
@RequestMapping("/api/admin/categorias")
@RequiredArgsConstructor
public class AdminCategoriaController {

    private final AdminFacade facade;

    @GetMapping
    public List<CategoriaResponse> listar(@UsuarioActual Long adminId) {
        return facade.categorias(adminId);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@UsuarioActual Long adminId,
                                                   @Valid @RequestBody CategoriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.crearCategoria(adminId, req));
    }

    @PutMapping("/{id}")
    public CategoriaResponse renombrar(@UsuarioActual Long adminId,
                                       @PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return facade.renombrarCategoria(adminId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@UsuarioActual Long adminId,
                                         @PathVariable Long id) {
        facade.eliminarCategoria(adminId, id);
        return ResponseEntity.noContent().build();
    }
}
