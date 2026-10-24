package com.poli.restaurante.controller.admin;

import com.poli.restaurante.dto.ProductoRequest;
import com.poli.restaurante.dto.ProductoResponse;
import com.poli.restaurante.service.AccesoService;
import com.poli.restaurante.service.AdminFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-11: el administrador crea, edita y elimina productos del menú. */
@RestController
@RequestMapping("/api/admin/productos")
@RequiredArgsConstructor
public class AdminProductoController {

    private final AdminFacade facade;

    /** Todos los productos, disponibles o no. */
    @GetMapping
    public List<ProductoResponse> listar(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId) {
        return facade.productos(adminId);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId,
                                                  @Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.crearProducto(adminId, req));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId,
                                       @PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return facade.actualizarProducto(adminId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestHeader(AccesoService.CABECERA_ADMIN) Long adminId,
                                         @PathVariable Long id) {
        facade.eliminarProducto(adminId, id);
        return ResponseEntity.noContent().build();
    }
}
