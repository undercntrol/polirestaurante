package com.poli.restaurante.repository;

import com.poli.restaurante.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByUsername(String username);
}
