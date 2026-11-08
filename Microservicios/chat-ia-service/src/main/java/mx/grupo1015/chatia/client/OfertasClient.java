package mx.grupo1015.chatia.client;

import mx.grupo1015.chatia.dto.OfertaVigente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Consulta el microservicio de ofertas y remates de la rama gene1 (microservicio-ofertas).
 * Igual que CatalogoClient: la dirección se pide a Eureka y, si no responde,
 * se usa la URL fija de la configuración.
 * Si el servicio está apagado regresa una lista vacía para que el chat siga funcionando.
 */
@Component
public class OfertasClient {

    private static final Logger log = LoggerFactory.getLogger(OfertasClient.class);

    private static final String SERVICIO_OFERTAS = "microservicio-ofertas";

    private final RestClient restClient;
    private final String urlOfertas;
    private final Optional<DiscoveryClient> discoveryClient;

    @Autowired
    public OfertasClient(
            @Value("${ofertas.url}") String urlOfertas,
            Optional<DiscoveryClient> discoveryClient
    ) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(fabrica)
                .build();
        this.urlOfertas = urlOfertas;
        this.discoveryClient = discoveryClient;
    }

    public OfertasClient(String urlOfertas) {
        this(urlOfertas, Optional.empty());
    }

    /**
     * Ofertas y remates vigentes de una propiedad (la clave es la misma que usa inmobiliaria.html).
     */
    public List<OfertaVigente> buscarVigentes(String clave) {
        LocalDate hoy = LocalDate.now();

        for (String base : direcciones()) {
            try {
                List<OfertaVigente> ofertas = restClient.get()
                        .uri(base + "/api/ofertas?propiedad={clave}", clave)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<OfertaVigente>>() {});

                if (ofertas == null) {
                    return List.of();
                }

                return ofertas.stream()
                        .filter(oferta -> oferta.vigenteEn(hoy))
                        .toList();

            } catch (ResourceAccessException exception) {
                // No hubo conexión con esa dirección: se intenta con la siguiente
                log.warn("No fue posible conectar con {} en {}: {}", SERVICIO_OFERTAS, base, exception.getMessage());

            } catch (RestClientException exception) {
                log.warn("No fue posible consultar {}: {}", SERVICIO_OFERTAS, exception.getMessage());
                return List.of();
            }
        }

        return List.of();
    }

    /**
     * Primero las direcciones que da Eureka y al final la URL fija de la configuración.
     */
    private Set<String> direcciones() {
        Set<String> direcciones = new LinkedHashSet<>();

        discoveryClient.ifPresent(cliente -> {
            try {
                cliente.getInstances(SERVICIO_OFERTAS)
                        .forEach(instancia -> direcciones.add(instancia.getUri().toString()));
            } catch (RuntimeException exception) {
                log.warn("No fue posible consultar Eureka: {}", exception.getMessage());
            }
        });

        direcciones.add(urlOfertas);
        return direcciones;
    }
}
