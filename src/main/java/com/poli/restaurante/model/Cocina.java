package com.poli.restaurante.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cocina")
@NoArgsConstructor
public class Cocina extends Usuario {

    @Override
    public Rol getRol() {
        return Rol.COCINA;
    }
}
