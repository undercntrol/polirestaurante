package com.poli.restaurante.service;

import com.poli.restaurante.exception.NoEncontradoException;
import com.poli.restaurante.exception.ReglaNegocioException;
import com.poli.restaurante.model.*;
import com.poli.restaurante.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    /** Estados en los que el pedido sigue "vivo" y la mesa sigue ocupada. */
    private static final List<EstadoPedido> ACTIVOS =
            List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);

    private static final List<EstadoPedido> COLA_COCINA =
            List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION);

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final MeseroRepository meseroRepository;
    private final CocinaRepository cocinaRepository;
    private final ProductoRepository productoRepository;
    private final HistorialEstadoRepository historialRepository;

    // ---------- Mesero ----------

    /**
     * HU-01: crea el pedido en PENDIENTE y deja el primer registro del historial.
     * En sitio: exige una mesa libre y la ocupa. Para llevar: no lleva mesa.
     */
    @Transactional
    public Pedido crearPedido(Long mesaId, Long meseroId, TipoPedido tipo) {
        Mesero mesero = buscarMesero(meseroId);
        if (tipo == null) {
            tipo = TipoPedido.EN_SITIO;
        }
        Mesa mesa = null;
        if (tipo == TipoPedido.EN_SITIO) {
            if (mesaId == null) {
                throw new ReglaNegocioException("Un pedido en sitio necesita una mesa");
            }
            mesa = mesaRepository.findById(mesaId)
                    .orElseThrow(() -> new NoEncontradoException("Mesa no encontrada: " + mesaId));
            if (mesa.getEstado() == EstadoMesa.OCUPADA) {
                String detalle = pedidoRepository.findFirstByMesaIdAndEstadoIn(mesaId, ACTIVOS)
                        .map(p -> " Pedido activo: " + p.getId())
                        .orElse("");
                throw new ReglaNegocioException("La mesa " + mesa.getNumero() + " ya está ocupada." + detalle);
            }
            mesa.setEstado(EstadoMesa.OCUPADA);
        } else if (mesaId != null) {
            throw new ReglaNegocioException("Un pedido para llevar no se asigna a una mesa");
        }

        Pedido pedido = new Pedido();
        pedido.setTipoPedido(tipo);
        pedido.setMesa(mesa);
        pedido.setMesero(mesero);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido = pedidoRepository.save(pedido);

        registrarHistorial(pedido, EstadoPedido.PENDIENTE, mesero);
        return pedido;
    }

    /** HU-02: agrega un producto disponible. Solo mientras el pedido está PENDIENTE. */
    @Transactional
    public Pedido agregarProducto(Long pedidoId, Long productoId, int cantidad, String notas) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirPendiente(pedido, "agregar productos");
        if (cantidad < 1) {
            throw new ReglaNegocioException("La cantidad debe ser al menos 1");
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado: " + productoId));
        if (!producto.isDisponible()) {
            throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' no está disponible");
        }

        DetallePedido detalle = new DetallePedido();
        detalle.setPedido(pedido);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setNotas(notas);
        detalle.setPrecioUnitario(producto.getPrecio());
        pedido.getDetalles().add(detalle);
        pedido.setFechaUltimaActualizacion(LocalDateTime.now());
        return pedido;
    }

    /** HU-02: quita una línea. Si cocina ya empezó, se rechaza. */
    @Transactional
    public Pedido quitarDetalle(Long pedidoId, Long detalleId) {
        Pedido pedido = buscarPedido(pedidoId);
        exigirPendiente(pedido, "quitar productos");
        boolean quitado = pedido.getDetalles().removeIf(d -> d.getId().equals(detalleId));
        if (!quitado) {
            throw new NoEncontradoException("El pedido no tiene la línea " + detalleId);
        }
        pedido.setFechaUltimaActualizacion(LocalDateTime.now());
        return pedido;
    }

    /** HU-06: registra el método de pago de un pedido entregado. */
    @Transactional
    public Pedido registrarPago(Long pedidoId, String metodoPago) {
        Pedido pedido = buscarPedido(pedidoId);
        if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException("Solo se registra el pago de pedidos entregados. Estado actual: "
                    + pedido.getEstado());
        }
        pedido.setMetodoPago(metodoPago);
        pedido.setPagado(true);
        pedido.setFechaUltimaActualizacion(LocalDateTime.now());
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<Pedido> pedidosActivosDelMesero(Long meseroId) {
        return pedidoRepository.findByMeseroIdAndEstadoIn(meseroId, ACTIVOS);
    }

    // ---------- Cocina ----------

    /** HU-03: pendientes y en preparación, del más antiguo al más reciente. */
    @Transactional(readOnly = true)
    public List<Pedido> colaDeCocina() {
        return pedidoRepository.findByEstadoInOrderByFechaCreacionAsc(COLA_COCINA);
    }

    // ---------- Cambio de estado ----------

    /** HU-05 y HU-07: cambios de estado que hace el mesero (entregar, cancelar). */
    @Transactional
    public Pedido cambiarEstadoPorMesero(Long pedidoId, EstadoPedido destino, Long meseroId) {
        return cambiarEstado(buscarPedido(pedidoId), destino, buscarMesero(meseroId));
    }

    /** HU-03 y HU-04: cambios de estado que hace cocina (en preparación, listo). */
    @Transactional
    public Pedido cambiarEstadoPorCocina(Long pedidoId, EstadoPedido destino, Long cocinaId) {
        return cambiarEstado(buscarPedido(pedidoId), destino, buscarCocina(cocinaId));
    }

    // ---------- Internos ----------

    /**
     * Único punto por donde cambia el estado de un pedido.
     * Valida el rol, valida la transición y registra el historial.
     */
    private Pedido cambiarEstado(Pedido pedido, EstadoPedido destino, Usuario actor) {
        validarRol(actor, destino);
        if (!pedido.getEstado().puedePasarA(destino)) {
            throw new ReglaNegocioException("No se puede pasar de " + pedido.getEstado() + " a " + destino
                    + ". Estado actual: " + pedido.getEstado());
        }
        pedido.setEstado(destino);
        pedido.setFechaUltimaActualizacion(LocalDateTime.now());
        registrarHistorial(pedido, destino, actor);

        if (destino == EstadoPedido.ENTREGADO || destino == EstadoPedido.CANCELADO) {
            liberarMesaSiNoHayActivos(pedido.getMesa());
        }
        return pedido;
    }

    private void validarRol(Usuario actor, EstadoPedido destino) {
        boolean permitido = switch (destino) {
            case EN_PREPARACION, LISTO -> actor.getRol() == Rol.COCINA;
            case ENTREGADO, CANCELADO -> actor.getRol() == Rol.MESERO;
            case PENDIENTE -> false;
        };
        if (!permitido) {
            throw new ReglaNegocioException("Un usuario con rol " + actor.getRol()
                    + " no puede pasar un pedido a " + destino);
        }
    }

    private void exigirPendiente(Pedido pedido, String accion) {
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ReglaNegocioException("No se pueden " + accion + " porque el pedido está "
                    + pedido.getEstado());
        }
    }

    private void liberarMesaSiNoHayActivos(Mesa mesa) {
        if (mesa == null) {
            return; // pedido para llevar
        }
        if (!pedidoRepository.existsByMesaIdAndEstadoIn(mesa.getId(), ACTIVOS)) {
            mesa.setEstado(EstadoMesa.LIBRE);
        }
    }

    /** Guarda quién hizo el cambio en la columna de su tabla (mesero_id o cocina_id). */
    private void registrarHistorial(Pedido pedido, EstadoPedido estado, Usuario actor) {
        HistorialEstado h = new HistorialEstado();
        h.setPedido(pedido);
        h.setEstado(estado);
        h.setFecha(LocalDateTime.now());
        if (actor instanceof Mesero mesero) {
            h.setMesero(mesero);
        } else if (actor instanceof Cocina cocina) {
            h.setCocina(cocina);
        } else {
            throw new ReglaNegocioException("Un " + actor.getRol() + " no cambia estados de pedidos");
        }
        historialRepository.save(h);
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Pedido no encontrado: " + id));
    }

    Mesero buscarMesero(Long id) {
        return exigirActivo(meseroRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Mesero no encontrado: " + id)));
    }

    Cocina buscarCocina(Long id) {
        return exigirActivo(cocinaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Usuario de cocina no encontrado: " + id)));
    }

    private <T extends Usuario> T exigirActivo(T usuario) {
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("La cuenta del usuario está inactiva");
        }
        return usuario;
    }
}
