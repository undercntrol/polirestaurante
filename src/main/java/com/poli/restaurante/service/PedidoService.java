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
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final HistorialEstadoRepository historialRepository;

    // ---------- Mesero ----------

    /** HU-01: crea el pedido en PENDIENTE, ocupa la mesa y deja el primer registro del historial. */
    @Transactional
    public Pedido crearPedido(Long mesaId, Long meseroId) {
        Usuario mesero = buscarUsuario(meseroId);
        if (mesero.getRol() != Rol.MESERO) {
            throw new ReglaNegocioException("Solo un mesero puede crear pedidos");
        }
        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new NoEncontradoException("Mesa no encontrada: " + mesaId));

        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            String detalle = pedidoRepository.findFirstByMesaIdAndEstadoIn(mesaId, ACTIVOS)
                    .map(p -> " Pedido activo: " + p.getId())
                    .orElse("");
            throw new ReglaNegocioException("La mesa " + mesa.getNumero() + " ya está ocupada." + detalle);
        }

        mesa.setEstado(EstadoMesa.OCUPADA);

        Pedido pedido = new Pedido();
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

    /** HU-03: registra el método de pago al entregar. */
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

    /** HU-04: pendientes y en preparación, del más antiguo al más reciente. */
    @Transactional(readOnly = true)
    public List<Pedido> colaDeCocina() {
        return pedidoRepository.findByEstadoInOrderByFechaCreacionAsc(COLA_COCINA);
    }

    // ---------- Cambio de estado (cocina, mesero) ----------

    /**
     * Único punto por donde cambia el estado de un pedido.
     * Valida el rol, valida la transición y registra el historial.
     */
    @Transactional
    public Pedido cambiarEstado(Long pedidoId, EstadoPedido destino, Long usuarioId) {
        Pedido pedido = buscarPedido(pedidoId);
        Usuario usuario = buscarUsuario(usuarioId);

        validarRol(usuario, destino);

        if (!pedido.getEstado().puedePasarA(destino)) {
            throw new ReglaNegocioException("No se puede pasar de " + pedido.getEstado() + " a " + destino);
        }

        pedido.setEstado(destino);
        pedido.setFechaUltimaActualizacion(LocalDateTime.now());
        registrarHistorial(pedido, destino, usuario);

        if (destino == EstadoPedido.ENTREGADO || destino == EstadoPedido.CANCELADO) {
            liberarMesaSiNoHayActivos(pedido.getMesa());
        }
        return pedido;
    }

    // ---------- Internos ----------

    private void validarRol(Usuario usuario, EstadoPedido destino) {
        boolean permitido = switch (destino) {
            case EN_PREPARACION, LISTO -> usuario.getRol() == Rol.COCINA;
            case ENTREGADO, CANCELADO -> usuario.getRol() == Rol.MESERO;
            case PENDIENTE -> false;
        };
        if (!permitido) {
            throw new ReglaNegocioException("Un usuario con rol " + usuario.getRol()
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
        if (!pedidoRepository.existsByMesaIdAndEstadoIn(mesa.getId(), ACTIVOS)) {
            mesa.setEstado(EstadoMesa.LIBRE);
        }
    }

    private void registrarHistorial(Pedido pedido, EstadoPedido estado, Usuario usuario) {
        HistorialEstado h = new HistorialEstado();
        h.setPedido(pedido);
        h.setEstado(estado);
        h.setUsuario(usuario);
        h.setFecha(LocalDateTime.now());
        historialRepository.save(h);
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Pedido no encontrado: " + id));
    }

    private Usuario buscarUsuario(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado: " + id));
        if (!u.isActivo()) {
            throw new ReglaNegocioException("La cuenta del usuario está inactiva");
        }
        return u;
    }
}
