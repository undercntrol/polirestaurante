package com.poli.restaurante.repository;

import com.poli.restaurante.model.Cocina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CocinaRepository extends JpaRepository<Cocina, Long> {

    Optional<Cocina> findByUsername(String username);
}
