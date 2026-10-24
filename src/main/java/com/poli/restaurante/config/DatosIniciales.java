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

    @Override
    public void run(String... args) {
        if (meseros.count() > 0) {
            return;
        }
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
