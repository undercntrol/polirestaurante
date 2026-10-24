package com.poli.restaurante.service;

import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.dto.ProductoRequest;
import com.poli.restaurante.model.Categoria;
import com.poli.restaurante.model.Cocina;
import com.poli.restaurante.model.Producto;
import com.poli.restaurante.repository.CategoriaRepository;
import com.poli.restaurante.repository.CocinaRepository;
import com.poli.restaurante.repository.DetallePedidoRepository;
import com.poli.restaurante.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CocinaRepository cocinaRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    /** HU-09: todos los productos, disponibles o no, para que cocina vea cuáles reactivar. */
    @Transactional(readOnly = true)
    public List<Producto> todos() {
        return productoRepository.findAll(Sort.by("nombre"));
    }

    /**
     * HU-09: cocina marca un producto como agotado o lo reactiva.
     * Un producto no disponible deja de salir en el menú de nuevos pedidos (GET /api/menu),
     * pero las líneas de pedidos ya creados no cambian.
     */
    @Transactional
    public Producto cambiarDisponibilidad(Long productoId, boolean disponible, Long cocinaId) {
        Cocina cocina = cocinaRepository.findById(cocinaId)
                .orElseThrow(() -> new NoEncontradoException("Usuario de cocina no encontrado: " + cocinaId));
        if (!cocina.isActivo()) {
            throw new ReglaNegocioException("La cuenta del usuario está inactiva");
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado: " + productoId));
        producto.setDisponible(disponible);
        return producto;
    }

    // ---------- Administrador (HU-11) ----------

    /** HU-11: crea el producto y queda disponible de inmediato en el menú (salvo que se envíe disponible=false). */
    @Transactional
    public Producto crear(ProductoRequest req) {
        Producto producto = new Producto();
        aplicar(producto, req);
        return productoRepository.save(producto);
    }

    /**
     * HU-11: edita nombre, descripción, precio, categoría o disponibilidad.
     * Los pedidos ya creados no cambian: cada línea guardó su propio precio_unitario.
     */
    @Transactional
    public Producto actualizar(Long productoId, ProductoRequest req) {
        Producto producto = buscar(productoId);
        aplicar(producto, req);
        return producto;
    }

    /** HU-11: solo se borra si nunca se ha pedido; si ya está en pedidos, se debe desactivar. */
    @Transactional
    public void eliminar(Long productoId) {
        Producto producto = buscar(productoId);
        if (detallePedidoRepository.existsByProductoId(productoId)) {
            throw new ReglaNegocioException("El producto '" + producto.getNombre()
                    + "' ya está en pedidos registrados. Márcalo como no disponible en lugar de eliminarlo.");
        }
        productoRepository.delete(producto);
    }

    private void aplicar(Producto producto, ProductoRequest req) {
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada: " + req.categoriaId()));
        producto.setNombre(req.nombre().trim());
        producto.setDescripcion(req.descripcion());
        producto.setPrecio(req.precio());
        producto.setCategoria(categoria);
        if (req.disponible() != null) {
            producto.setDisponible(req.disponible());
        }
    }

    private Producto buscar(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado: " + productoId));
    }
}
