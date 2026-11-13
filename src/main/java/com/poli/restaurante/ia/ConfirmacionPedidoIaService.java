package com.poli.restaurante.ia;

import com.poli.restaurante.dto.ItemConfirmado;
import com.poli.restaurante.dto.PedidoResponse;
import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.Pedido;
import com.poli.restaurante.model.TipoPedido;
import com.poli.restaurante.repository.MesaRepository;
import com.poli.restaurante.repository.PedidoRepository;
import com.poli.restaurante.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Crea el pedido a partir de la propuesta del asistente usando las mismas reglas de siempre
 * (PedidoService): mesa libre, productos disponibles, historial de estados. Si cualquier línea falla,
 * no se crea nada (todo va en una sola transacción).
 */
@Service
@RequiredArgsConstructor
public class ConfirmacionPedidoIaService {

    private final PedidoService pedidoService;
    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional
    public PedidoResponse confirmar(Long meseroId, TipoPedido tipo, Integer mesaNumero, List<ItemConfirmado> items) {
        if (items == null || items.isEmpty()) {
            throw new ReglaNegocioException("El pedido no tiene productos");
        }
        TipoPedido t = tipo == null ? TipoPedido.EN_SITIO : tipo;
        Long mesaId = null;
        if (t == TipoPedido.EN_SITIO) {
            if (mesaNumero == null) {
                throw new ReglaNegocioException("Indica la mesa del pedido");
            }
            mesaId = mesaRepository.findByNumero(mesaNumero)
                    .orElseThrow(() -> new NoEncontradoException("La mesa " + mesaNumero + " no existe"))
                    .getId();
        }
        Pedido pedido = pedidoService.crearPedido(mesaId, meseroId, t);
        for (ItemConfirmado it : items) {
            String notas = it.notas() == null || it.notas().isBlank() ? null : it.notas().trim();
            pedido = pedidoService.agregarProducto(pedido.getId(), it.productoId(), it.cantidad(), notas);
        }
        pedidoRepository.flush();
        return PedidoResponse.de(pedido);
    }
}
