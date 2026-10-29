package mx.grupo1015.chatia.client;

import mx.grupo1015.chatia.dto.PropiedadCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Consulta los microservicios de catálogo de la rama pablo:
 * primero inmuebles-service (casas) y, si no tiene la clave, terrenos-service.
 * La dirección de cada uno se pide a Eureka; si Eureka no lo conoce o no responde,
 * se usa la URL fija de la configuración.
 * Si ninguno responde o no tienen la propiedad, regresa vacío para que
 * el chat siga funcionando sin esos datos.
 */
@Component
public class CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);

    private static final String SERVICIO_INMUEBLES = "inmuebles-service";
    private static final String SERVICIO_TERRENOS = "terrenos-service";

    private final RestClient restClient;
    private final String urlInmuebles;
    private final String urlTerrenos;
    private final Optional<DiscoveryClient> discoveryClient;

    @Autowired
    public CatalogoClient(
            @Value("${catalogo.inmuebles-url}") String urlInmuebles,
            @Value("${catalogo.terrenos-url}") String urlTerrenos,
            Optional<DiscoveryClient> discoveryClient
    ) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(fabrica)
                .build();
        this.urlInmuebles = urlInmuebles;
        this.urlTerrenos = urlTerrenos;
        this.discoveryClient = discoveryClient;
    }

    public CatalogoClient(String urlInmuebles, String urlTerrenos) {
        this(urlInmuebles, urlTerrenos, Optional.empty());
    }

    public Optional<PropiedadCatalogo> buscarPorClave(String clave) {
        return buscar(SERVICIO_INMUEBLES, urlInmuebles, "/api/inmuebles/clave/{clave}", clave)
                .map(propiedad -> propiedad.conCategoria("CASA"))
                .or(() -> buscar(SERVICIO_TERRENOS, urlTerrenos, "/api/terrenos/clave/{clave}", clave)
                        .map(propiedad -> propiedad.conCategoria("TERRENO")));
    }

    private Optional<PropiedadCatalogo> buscar(
            String servicio,
            String urlFija,
            String ruta,
            String clave
    ) {
        for (String base : direcciones(servicio, urlFija)) {
            try {
                PropiedadCatalogo propiedad = restClient.get()
                        .uri(base + ruta, clave)
                        .retrieve()
                        .body(PropiedadCatalogo.class);

                return Optional.ofNullable(propiedad);

            } catch (HttpClientErrorException.NotFound exception) {
                log.debug("{} no tiene la propiedad con clave {}", servicio, clave);
                return Optional.empty();

            } catch (ResourceAccessException exception) {
                // No hubo conexión con esa dirección: se intenta con la siguiente
                log.warn("No fue posible conectar con {} en {}: {}", servicio, base, exception.getMessage());

            } catch (RestClientException exception) {
                log.warn("No fue posible consultar {}: {}", servicio, exception.getMessage());
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Primero las direcciones que da Eureka y al final la URL fija de la configuración.
     */
    private Set<String> direcciones(String servicio, String urlFija) {
        Set<String> direcciones = new LinkedHashSet<>();

        discoveryClient.ifPresent(cliente -> {
            try {
                cliente.getInstances(servicio)
                        .forEach(instancia -> direcciones.add(instancia.getUri().toString()));
            } catch (RuntimeException exception) {
                log.warn("No fue posible consultar Eureka: {}", exception.getMessage());
            }
        });

        direcciones.add(urlFija);
        return direcciones;
    }
}
