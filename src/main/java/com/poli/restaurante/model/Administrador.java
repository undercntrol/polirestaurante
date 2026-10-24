package com.poli.restaurante.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "administrador")
@NoArgsConstructor
public class Administrador extends Usuario {

    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }
}
