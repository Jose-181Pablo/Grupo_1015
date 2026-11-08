package com.grupo1015.microservicioofertas.client;

import com.grupo1015.microservicioofertas.exception.NoAutorizadoException;
import com.grupo1015.microservicioofertas.exception.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Pregunta al microservicio autenticacion (Liz) de quién es un token:
 * GET /api/auth/validar con el encabezado Authorization: Bearer <token>.
 * La dirección se pide a Eureka y, si no responde, se usa autenticacion.url.
 */
@Component
public class AutenticacionClient {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionClient.class);
    private static final String SERVICIO = "autenticacion";

    private final RestClient restClient;
    private final String urlAutenticacion;
    private final Optional<DiscoveryClient> discoveryClient;

    @Autowired
    public AutenticacionClient(@Value("${autenticacion.url}") String urlAutenticacion,
                               Optional<DiscoveryClient> discoveryClient) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
        this.urlAutenticacion = urlAutenticacion;
        this.discoveryClient = discoveryClient;
    }

    /** Cuenta dueña del token. Lanza 401 si el token no sirve y 503 si autenticación no responde. */
    public CuentaValidada validar(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new NoAutorizadoException("Inicia sesión con una cuenta de administrador.");
        }

        for (String base : direcciones()) {
            try {
                CuentaValidada cuenta = restClient.get()
                        .uri(base + "/api/auth/validar")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .retrieve()
                        .body(CuentaValidada.class);
                if (cuenta == null) {
                    throw new NoAutorizadoException("No se pudo validar la sesión.");
                }
                return cuenta;
            } catch (HttpClientErrorException e) {
                // 401/403 de autenticación: el token no es válido
                throw new NoAutorizadoException("Tu sesión no es válida. Vuelve a iniciar sesión.");
            } catch (RestClientException e) {
                log.warn("No se pudo validar el token en {}: {}", base, e.getMessage());
            }
        }
        throw new ServicioNoDisponibleException(
                "El microservicio de autenticación no está disponible. Intenta más tarde.");
    }

    private Set<String> direcciones() {
        Set<String> direcciones = new LinkedHashSet<>();
        discoveryClient.ifPresent(cliente -> {
            try {
                cliente.getInstances(SERVICIO)
                        .forEach(instancia -> direcciones.add(instancia.getUri().toString()));
            } catch (RuntimeException e) {
                log.debug("Eureka no respondió: {}", e.getMessage());
            }
        });
        direcciones.add(urlAutenticacion);
        return direcciones;
    }
}
