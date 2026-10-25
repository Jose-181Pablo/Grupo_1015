package mx.grupo1015.chatia.client;

import mx.grupo1015.chatia.dto.PropiedadCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/**
 * Consulta los microservicios de catálogo de la rama pablo:
 * primero inmuebles-service (casas) y, si no tiene la clave, terrenos-service.
 * Si ninguno responde o no tienen la propiedad, regresa vacío para que
 * el chat siga funcionando sin esos datos.
 */
@Component
public class CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);

    private final RestClient inmuebles;
    private final RestClient terrenos;

    public CatalogoClient(
            @Value("${catalogo.inmuebles-url}") String urlInmuebles,
            @Value("${catalogo.terrenos-url}") String urlTerrenos
    ) {
        this.inmuebles = crearCliente(urlInmuebles);
        this.terrenos = crearCliente(urlTerrenos);
    }

    public Optional<PropiedadCatalogo> buscarPorClave(String clave) {
        return buscar(inmuebles, "/api/inmuebles/clave/{clave}", clave, "inmuebles")
                .map(propiedad -> propiedad.conCategoria("CASA"))
                .or(() -> buscar(terrenos, "/api/terrenos/clave/{clave}", clave, "terrenos")
                        .map(propiedad -> propiedad.conCategoria("TERRENO")));
    }

    private Optional<PropiedadCatalogo> buscar(
            RestClient cliente,
            String ruta,
            String clave,
            String servicio
    ) {
        try {
            PropiedadCatalogo propiedad = cliente.get()
                    .uri(ruta, clave)
                    .retrieve()
                    .body(PropiedadCatalogo.class);

            return Optional.ofNullable(propiedad);

        } catch (HttpClientErrorException.NotFound exception) {
            log.debug("El catálogo de {} no tiene la propiedad con clave {}", servicio, clave);
            return Optional.empty();

        } catch (RestClientException exception) {
            log.warn("No fue posible consultar el catálogo de {}: {}", servicio, exception.getMessage());
            return Optional.empty();
        }
    }

    private static RestClient crearCliente(String url) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .build();
    }
}
