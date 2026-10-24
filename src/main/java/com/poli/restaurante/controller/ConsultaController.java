package com.poli.restaurante.controller;

import com.poli.restaurante.dto.MesaResponse;
import com.poli.restaurante.dto.ProductoResponse;
import com.poli.restaurante.service.PedidoFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lecturas de apoyo para armar un pedido: qué mesas hay y qué productos están disponibles. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsultaController {

    private final PedidoFacade facade;

    @GetMapping("/menu")
    public List<ProductoResponse> menu() {
        return facade.menu();
    }

    @GetMapping("/mesas")
    public List<MesaResponse> mesas() {
        return facade.mesas();
    }
}
