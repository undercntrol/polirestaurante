package com.poli.restaurante.repository;

import com.poli.restaurante.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    /** HU-11: un producto que ya está en pedidos no se borra; se desactiva. */
    boolean existsByProductoId(Long productoId);
}
