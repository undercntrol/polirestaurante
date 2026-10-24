package com.poli.restaurante.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Registro de solo lectura: se inserta una vez y no se edita ni se borra (RNF-08). */
@Entity
@Table(name = "historial_estado")
@Getter @Setter @NoArgsConstructor
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", updatable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EstadoPedido estado;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    // Quién hizo el cambio. Cada actor tiene su tabla, así que hay una columna por actor
    // y en cada registro se llena exactamente una de ellas.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id", updatable = false)
    private Mesero mesero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocina_id", updatable = false)
    private Cocina cocina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", updatable = false)
    private Cliente cliente;
}
