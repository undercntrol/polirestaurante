package com.poli.restaurante.repository;

import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Cola de cocina: del más antiguo al más reciente. */
    List<Pedido> findByEstadoInOrderByFechaCreacionAsc(Collection<EstadoPedido> estados);

    /** HU-08: pedidos activos de un mesero, del más antiguo al más reciente. */
    List<Pedido> findByMeseroIdAndEstadoInOrderByFechaCreacionAsc(Long meseroId, Collection<EstadoPedido> estados);

    Optional<Pedido> findFirstByMesaIdAndEstadoIn(Long mesaId, Collection<EstadoPedido> estados);

    boolean existsByMesaIdAndEstadoIn(Long mesaId, Collection<EstadoPedido> estados);

    /** HU-15: pedidos creados en un rango [inicio, fin). */
    List<Pedido> findByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(LocalDateTime inicio, LocalDateTime fin);

    /** HU-13: una mesa con pedidos en el historial no se borra. */
    boolean existsByMesaId(Long mesaId);
}
