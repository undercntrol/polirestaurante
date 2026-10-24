package com.poli.restaurante.controller;

import com.poli.restaurante.dto.*;
import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.service.PedidoFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** HU-01, HU-02, HU-05 y HU-06: lo que hace el mesero sobre un pedido. */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoFacade facade;

    /** HU-01: crear pedido y asignar mesa. */
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.crear(req));
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return facade.obtener(id);
    }

    /** HU-02: agregar un producto (con cantidad y notas opcionales). */
    @PostMapping("/{id}/detalles")
    public PedidoResponse agregarProducto(@PathVariable Long id, @Valid @RequestBody AgregarProductoRequest req) {
        return facade.agregarProducto(id, req);
    }

    /** HU-02: quitar una línea del pedido. */
    @DeleteMapping("/{id}/detalles/{detalleId}")
    public PedidoResponse quitarProducto(@PathVariable Long id, @PathVariable Long detalleId) {
        return facade.quitarDetalle(id, detalleId);
    }

    /** HU-05: marcar como entregado. */
    @PutMapping("/{id}/entregar")
    public PedidoResponse entregar(@PathVariable Long id, @Valid @RequestBody MeseroRequest req) {
        return facade.cambiarEstadoPorMesero(id, EstadoPedido.ENTREGADO, req.meseroId());
    }

    /** HU-06: registrar el método de pago de un pedido entregado. */
    @PutMapping("/{id}/pago")
    public PedidoResponse registrarPago(@PathVariable Long id, @Valid @RequestBody RegistrarPagoRequest req) {
        return facade.registrarPago(id, req);
    }
}
