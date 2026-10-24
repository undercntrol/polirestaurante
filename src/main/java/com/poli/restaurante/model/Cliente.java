package com.poli.restaurante.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Comensal (alcance extendido). Solo guarda datos de contacto; no inicia sesión con usuario y contraseña. */
@Entity
@Table(name = "cliente")
@Getter @Setter @NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String correo;

    private String telefono;
}
