package com.poli.restaurante.service;

import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.EstadoMesa;
import com.poli.restaurante.model.EstadoPedido;
import com.poli.restaurante.model.Mesa;
import com.poli.restaurante.repository.MesaRepository;
import com.poli.restaurante.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** HU-13: mesas del salón. */
@Service
@RequiredArgsConstructor
public class MesaService {

    private static final List<EstadoPedido> ACTIVOS =
            List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;

    /** HU-13: número y estado (libre u ocupada) de cada mesa. */
    @Transactional(readOnly = true)
    public List<Mesa> todas() {
        return mesaRepository.findAll(Sort.by("numero"));
    }

    /** HU-13: la mesa nueva queda LIBRE y lista para asignar pedidos. */
    @Transactional
    public Mesa crear(Integer numero) {
        if (mesaRepository.existsByNumero(numero)) {
            throw new ReglaNegocioException("Ya existe la mesa " + numero);
        }
        Mesa mesa = new Mesa();
        mesa.setNumero(numero);
        mesa.setEstado(EstadoMesa.LIBRE);
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa cambiarNumero(Long mesaId, Integer numero) {
        Mesa mesa = buscar(mesaId);
        if (mesaRepository.existsByNumeroAndIdNot(numero, mesaId)) {
            throw new ReglaNegocioException("Ya existe la mesa " + numero);
        }
        mesa.setNumero(numero);
        return mesa;
    }

    /**
     * HU-13: no se elimina una mesa con un pedido activo (hasta que se entregue o se cancele).
     * Tampoco una que ya tiene pedidos en el historial, para no perder ese registro.
     */
    @Transactional
    public void eliminar(Long mesaId) {
        Mesa mesa = buscar(mesaId);
        if (pedidoRepository.existsByMesaIdAndEstadoIn(mesaId, ACTIVOS)) {
            throw new ReglaNegocioException("La mesa " + mesa.getNumero()
                    + " tiene un pedido activo. Se puede eliminar cuando ese pedido se entregue o se cancele.");
        }
        if (pedidoRepository.existsByMesaId(mesaId)) {
            throw new ReglaNegocioException("La mesa " + mesa.getNumero()
                    + " tiene pedidos en el historial y se conserva para no perder ese registro.");
        }
        mesaRepository.delete(mesa);
    }

    private Mesa buscar(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Mesa no encontrada: " + id));
    }
}
