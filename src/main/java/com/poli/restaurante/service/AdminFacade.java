package com.poli.restaurante.service;

import com.poli.restaurante.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fachada del módulo de administración: verifica que quien llama sea administrador,
 * abre la transacción y convierte las entidades a DTO antes de cerrarla.
 */
@Service
@RequiredArgsConstructor
public class AdminFacade {

    private final AccesoService acceso;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    // ----- HU-11: productos -----

    @Transactional(readOnly = true)
    public List<ProductoResponse> productos(Long adminId) {
        acceso.exigirAdministrador(adminId);
        return productoService.todos().stream().map(ProductoResponse::de).toList();
    }

    @Transactional
    public ProductoResponse crearProducto(Long adminId, ProductoRequest req) {
        acceso.exigirAdministrador(adminId);
        return ProductoResponse.de(productoService.crear(req));
    }

    @Transactional
    public ProductoResponse actualizarProducto(Long adminId, Long productoId, ProductoRequest req) {
        acceso.exigirAdministrador(adminId);
        return ProductoResponse.de(productoService.actualizar(productoId, req));
    }

    @Transactional
    public void eliminarProducto(Long adminId, Long productoId) {
        acceso.exigirAdministrador(adminId);
        productoService.eliminar(productoId);
    }

    // ----- HU-12: categorías -----

    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias(Long adminId) {
        acceso.exigirAdministrador(adminId);
        return categoriaService.todas().stream()
                .map(c -> CategoriaResponse.de(c, categoriaService.cantidadProductos(c.getId())))
                .toList();
    }

    @Transactional
    public CategoriaResponse crearCategoria(Long adminId, CategoriaRequest req) {
        acceso.exigirAdministrador(adminId);
        return CategoriaResponse.de(categoriaService.crear(req.nombre()), 0);
    }

    @Transactional
    public CategoriaResponse renombrarCategoria(Long adminId, Long categoriaId, CategoriaRequest req) {
        acceso.exigirAdministrador(adminId);
        var c = categoriaService.renombrar(categoriaId, req.nombre());
        return CategoriaResponse.de(c, categoriaService.cantidadProductos(categoriaId));
    }

    @Transactional
    public void eliminarCategoria(Long adminId, Long categoriaId) {
        acceso.exigirAdministrador(adminId);
        categoriaService.eliminar(categoriaId);
    }
}
