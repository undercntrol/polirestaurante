package com.poli.restaurante.service;

import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.Categoria;
import com.poli.restaurante.repository.CategoriaRepository;
import com.poli.restaurante.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** HU-12: categorías del menú. */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<Categoria> todas() {
        return categoriaRepository.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public long cantidadProductos(Long categoriaId) {
        return productoRepository.countByCategoriaId(categoriaId);
    }

    /** HU-12: la categoría nueva queda disponible para asignarla a productos. */
    @Transactional
    public Categoria crear(String nombre) {
        String limpio = nombre.trim();
        if (categoriaRepository.existsByNombreIgnoreCase(limpio)) {
            throw new ReglaNegocioException("Ya existe una categoría llamada '" + limpio + "'");
        }
        Categoria c = new Categoria();
        c.setNombre(limpio);
        return categoriaRepository.save(c);
    }

    /** HU-12: los productos siguen asociados por id, así que muestran el nombre nuevo sin hacer nada más. */
    @Transactional
    public Categoria renombrar(Long categoriaId, String nombre) {
        Categoria c = buscar(categoriaId);
        String limpio = nombre.trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(limpio, categoriaId)) {
            throw new ReglaNegocioException("Ya existe una categoría llamada '" + limpio + "'");
        }
        c.setNombre(limpio);
        return c;
    }

    /** HU-12: no se elimina si tiene productos; hay que reasignarlos primero. */
    @Transactional
    public void eliminar(Long categoriaId) {
        Categoria c = buscar(categoriaId);
        long productos = productoRepository.countByCategoriaId(categoriaId);
        if (productos > 0) {
            throw new ReglaNegocioException("La categoría '" + c.getNombre() + "' tiene " + productos
                    + " producto(s). Reasígnalos a otra categoría antes de eliminarla.");
        }
        categoriaRepository.delete(c);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada: " + id));
    }
}
