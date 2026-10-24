package com.poli.restaurante.repository;

import com.poli.restaurante.model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {

    List<MetodoPago> findByActivoTrueOrderByNombreAsc();
}
