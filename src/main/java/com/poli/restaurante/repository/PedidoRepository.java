package com.poli.restaurante.repository;

import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Cola de cocina: del más antiguo al más reciente. */
    List<Pedido> findByEstadoInOrderByFechaCreacionAsc(Collection<EstadoPedido> estados);

    /** Pedidos activos de un mesero. */
    List<Pedido> findByMeseroIdAndEstadoIn(Long meseroId, Collection<EstadoPedido> estados);

    Optional<Pedido> findFirstByMesaIdAndEstadoIn(Long mesaId, Collection<EstadoPedido> estados);

    boolean existsByMesaIdAndEstadoIn(Long mesaId, Collection<EstadoPedido> estados);
}
