package com.poli.restaurante.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mesero")
@NoArgsConstructor
public class Mesero extends Usuario {

    @Override
    public Rol getRol() {
        return Rol.MESERO;
    }
}
