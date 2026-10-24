package com.poli.restaurante.service;

import com.poli.restaurante.dto.*;
import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.Pedido;
import com.poli.restaurante.repository.MesaRepository;
import com.poli.restaurante.repository.PedidoRepository;
import com.poli.restaurante.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Capa entre los controladores y PedidoService. Abre la transacción y convierte
 * las entidades a DTO antes de cerrarla, porque open-in-view está desactivado
 * y las relaciones LAZY no se pueden leer fuera de ella.
 */
@Service
@RequiredArgsConstructor
public class PedidoFacade {

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final MesaRepository mesaRepository;

    // ----- Mesero -----

    @Transactional
    public PedidoResponse crear(Long mesaId, Long meseroId) {
        return PedidoResponse.de(pedidoService.crearPedido(mesaId, meseroId));
    }

    @Transactional
    public PedidoResponse agregarProducto(Long pedidoId, AgregarProductoRequest req) {
        int cantidad = req.cantidad() == null ? 1 : req.cantidad();
        Pedido pedido = pedidoService.agregarProducto(pedidoId, req.productoId(), cantidad, req.notas());
        pedidoRepository.flush(); // para que la línea nueva ya tenga su id al responder
        return PedidoResponse.de(pedido);
    }

    @Transactional
    public PedidoResponse quitarDetalle(Long pedidoId, Long detalleId) {
        return PedidoResponse.de(pedidoService.quitarDetalle(pedidoId, detalleId));
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoEncontradoException("Pedido no encontrado: " + pedidoId));
        return PedidoResponse.de(pedido);
    }

    // ----- Cocina -----

    @Transactional(readOnly = true)
    public List<PedidoResponse> cola() {
        return pedidoService.colaDeCocina().stream().map(PedidoResponse::de).toList();
    }

    // ----- Cambios de estado (cocina y mesero) -----

    @Transactional
    public PedidoResponse cambiarEstado(Long pedidoId, EstadoPedido destino, Long usuarioId) {
        return PedidoResponse.de(pedidoService.cambiarEstado(pedidoId, destino, usuarioId));
    }

    // ----- Consultas de apoyo para el frontend del mesero -----

    @Transactional(readOnly = true)
    public List<ProductoResponse> menu() {
        return productoRepository.findByDisponibleTrue().stream().map(ProductoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MesaResponse> mesas() {
        return mesaRepository.findAll().stream().map(MesaResponse::de).toList();
    }
}
