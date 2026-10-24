package com.poli.restaurante.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos comunes de los actores que inician sesión (mesero, cocina y administrador).
 * No es una tabla: cada actor tiene la suya, como pide el modelo relacional
 * (herencia "una tabla por clase concreta").
 */
@MappedSuperclass
@Getter @Setter @NoArgsConstructor
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo = true;

    /** El rol sale del tipo de actor; no se guarda en la base de datos. */
    public abstract Rol getRol();
}
