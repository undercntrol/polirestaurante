package com.poli.restaurante.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Registra los intérpretes de pedidos como componentes de Spring. */
@Configuration
public class IaConfig {

    @Bean
    public InterpreteReglas interpreteReglas() {
        return new InterpreteReglas();
    }

    /**
     * Gemini queda registrado aunque no haya clave; en ese caso disponible() es false
     * y el asistente usa el intérprete por reglas.
     */
    @Bean
    public InterpreteGemini interpreteGemini(@Value("${ia.gemini.api-key:}") String apiKey,
                                             @Value("${ia.gemini.modelo:gemini-3.8-flash}") String modelo,
                                             @Value("${ia.gemini.url:https://generativelanguage.googleapis.com}") String url,
                                             @Value("${ia.gemini.tiempo-maximo-segundos:30}") long segundos) {
        return new InterpreteGemini(apiKey, modelo, url, Duration.ofSeconds(segundos));
    }
}
