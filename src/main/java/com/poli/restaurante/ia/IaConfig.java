package com.poli.restaurante.ia;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los intérpretes de pedidos como componentes de Spring. */
@Configuration
public class IaConfig {

    @Bean
    public InterpreteReglas interpreteReglas() {
        return new InterpreteReglas();
    }
}
