package com.poli.restaurante.repository;

import com.poli.restaurante.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
    /** HU-10: en orden cronológico (el id desempata cambios del mismo instante). */
    List<HistorialEstado> findByPedidoIdOrderByFechaAscIdAsc(Long pedidoId);
}
