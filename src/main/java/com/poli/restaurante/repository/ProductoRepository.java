package com.poli.restaurante.repository;

import com.poli.restaurante.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByDisponibleTrue();

    /** HU-12: para no borrar categorías que tienen productos. */
    long countByCategoriaId(Long categoriaId);
}
