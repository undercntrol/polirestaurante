package com.poli.restaurante.repository;

import com.poli.restaurante.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
    List<HistorialEstado> findByPedidoIdOrderByFechaAsc(Long pedidoId);
}
