package com.poli.restaurante.repository;

import com.poli.restaurante.model.Mesero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeseroRepository extends JpaRepository<Mesero, Long> {

    Optional<Mesero> findByUsername(String username);
}
