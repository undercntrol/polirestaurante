package com.poli.restaurante.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Métodos de pago que acepta el restaurante (efectivo, tarjeta, etc.). El administrador los activa o desactiva. */
@Entity
@Table(name = "metodo_pago")
@Getter @Setter @NoArgsConstructor
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;
}
