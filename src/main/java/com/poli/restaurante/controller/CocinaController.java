package com.poli.restaurante.controller;

import com.poli.restaurante.dto.PedidoResponse;
import com.poli.restaurante.dto.CocinaRequest;
import com.poli.restaurante.dto.DisponibilidadRequest;
import com.poli.restaurante.dto.ProductoResponse;
import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.service.PedidoFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-03, HU-04 y HU-09: lo que hace cocina. */
@RestController
@RequestMapping("/api/cocina")
@RequiredArgsConstructor
public class CocinaController {

    private final PedidoFacade facade;

    /** HU-03: cola de pedidos pendientes y en preparación, del más antiguo al más reciente. */
    @GetMapping("/pedidos")
    public List<PedidoResponse> cola() {
        return facade.cola();
    }

    /** HU-03: tomar un pedido (pasa a EN_PREPARACION). */
    @PutMapping("/pedidos/{id}/preparar")
    public PedidoResponse preparar(@PathVariable Long id, @Valid @RequestBody CocinaRequest req) {
        return facade.cambiarEstadoPorCocina(id, EstadoPedido.EN_PREPARACION, req.cocinaId());
    }

    /** HU-04: marcar como listo. */
    @PutMapping("/pedidos/{id}/listo")
    public PedidoResponse listo(@PathVariable Long id, @Valid @RequestBody CocinaRequest req) {
        return facade.cambiarEstadoPorCocina(id, EstadoPedido.LISTO, req.cocinaId());
    }

    /** HU-09: todos los productos con su disponibilidad. */
    @GetMapping("/productos")
    public List<ProductoResponse> productos() {
        return facade.productosParaCocina();
    }

    /** HU-09: marcar un producto como agotado (disponible=false) o reactivarlo (disponible=true). */
    @PutMapping("/productos/{id}/disponibilidad")
    public ProductoResponse disponibilidad(@PathVariable Long id, @Valid @RequestBody DisponibilidadRequest req) {
        return facade.cambiarDisponibilidad(id, req);
    }
}
