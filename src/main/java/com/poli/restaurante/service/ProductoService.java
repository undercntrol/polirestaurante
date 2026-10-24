package com.poli.restaurante.service;

import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.Cocina;
import com.poli.restaurante.model.Producto;
import com.poli.restaurante.repository.CocinaRepository;
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
}
