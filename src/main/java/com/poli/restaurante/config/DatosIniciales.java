package com.poli.restaurante.config;

import com.poli.restaurante.model.*;
import com.poli.restaurante.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Datos de prueba para poder usar la API desde el primer arranque. Solo se cargan si no hay meseros. */
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final MeseroRepository meseros;
    private final CocinaRepository cocinas;
    private final AdministradorRepository administradores;
    private final MesaRepository mesas;
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final MetodoPagoRepository metodosPago;

    @Override
    public void run(String... args) {
        cargarMetodosPago();
        if (meseros.count() == 0) {
            cargarDatosBase();
        }
        cargarMenuAmpliado();
        cargarMesas();
    }

    private void cargarDatosBase() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        meseros.save(usuario(new Mesero(), "Mesero Demo", "mesero", encoder.encode("1234")));
        cocinas.save(usuario(new Cocina(), "Cocina Demo", "cocina", encoder.encode("1234")));
        administradores.save(usuario(new Administrador(), "Admin Demo", "admin", encoder.encode("1234")));

        for (int i = 1; i <= 5; i++) {
            Mesa m = new Mesa();
            m.setNumero(i);
            mesas.save(m);
        }

        Categoria platos = categoria("Platos fuertes");
        Categoria bebidas = categoria("Bebidas");
        productos.save(producto("Hamburguesa clásica", "Carne, queso y vegetales", "18000", platos));
        productos.save(producto("Perro caliente", "Con papas en fósforo", "12000", platos));
        productos.save(producto("Gaseosa", "Botella personal", "4000", bebidas));
        productos.save(producto("Jugo natural", "Del día", "6000", bebidas));
    }

    private <T extends Usuario> T usuario(T u, String nombre, String username, String hash) {
        u.setNombre(nombre);
        u.setUsername(username);
        u.setPasswordHash(hash);
        return u;
    }

    /** Va aparte para que también se carguen en bases de datos que ya tenían usuarios. */
    private void cargarMetodosPago() {
        if (metodosPago.count() > 0) {
            return;
        }
        for (String nombre : new String[]{"Efectivo", "Tarjeta débito", "Tarjeta crédito", "Nequi"}) {
            MetodoPago m = new MetodoPago();
            m.setNombre(nombre);
            metodosPago.save(m);
        }
    }

    /**
     * Menú más completo para el asistente de pedidos con IA. Solo agrega los productos que falten
     * (por nombre), así que también funciona en bases de datos que ya existían.
     */
    private void cargarMenuAmpliado() {
        String[][] menu = {
                {"Hamburguesa clásica", "Carne, queso y vegetales", "18000", "Platos fuertes"},
                {"Perro caliente", "Con papas en fósforo", "12000", "Platos fuertes"},
                {"Salchipapa", "Papas, salchicha y salsas", "15000", "Platos fuertes"},
                {"Papas a la francesa", "Porción mediana", "7000", "Entradas"},
                {"Nuggets de pollo", "Seis unidades", "12000", "Entradas"},
                {"Gaseosa", "Botella personal", "4000", "Bebidas"},
                {"Jugo natural", "Del día", "6000", "Bebidas"},
                {"Limonada natural", "Vaso de 12 onzas", "6000", "Bebidas"},
                {"Agua", "Botella personal", "3500", "Bebidas"},
                {"Malteada de vainilla", "Vaso de 12 onzas", "11000", "Postres"},
                {"Brownie con helado", "Brownie tibio con bola de vainilla", "10500", "Postres"},
        };
        for (String[] p : menu) {
            if (productos.existsByNombreIgnoreCase(p[0])) {
                continue;
            }
            Categoria c = categorias.findByNombreIgnoreCase(p[3]).orElseGet(() -> categoria(p[3]));
            productos.save(producto(p[0], p[1], p[2], c));
        }
    }

    /** Mesas 1 a 12, para poder probar el asistente con frases que mencionan mesas más altas. */
    private void cargarMesas() {
        for (int i = 1; i <= 12; i++) {
            if (!mesas.existsByNumero(i)) {
                Mesa m = new Mesa();
                m.setNumero(i);
                mesas.save(m);
            }
        }
    }

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return categorias.save(c);
    }

    private Producto producto(String nombre, String descripcion, String precio, Categoria categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        p.setCategoria(categoria);
        return p;
    }
}
