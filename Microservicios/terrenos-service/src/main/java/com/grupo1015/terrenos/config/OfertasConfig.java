package com.grupo1015.terrenos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * CONFIGURACIÓN: crea el cliente HTTP que usa el microservicio de ofertas.
 * Tiene timeout de 3 segundos para conectar y para leer la respuesta,
 * así si el otro servicio tarda o está caído, el nuestro no se queda esperando.
 */
@Configuration
public class OfertasConfig {

    @Bean
    public RestClient ofertasRestClient() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder()
                .requestFactory(fabrica)
                .build();
    }
}
