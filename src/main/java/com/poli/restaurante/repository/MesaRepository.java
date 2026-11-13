package com.poli.restaurante.repository;

import com.poli.restaurante.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    boolean existsByNumero(Integer numero);

    /** Asistente de pedidos: la IA entiende el número de mesa, no su id. */
    Optional<Mesa> findByNumero(Integer numero);

    boolean existsByNumeroAndIdNot(Integer numero, Long id);
}
