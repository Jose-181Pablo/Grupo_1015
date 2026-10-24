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
 * Consulta el microservicio de catálogo (inmobiliaria-service, rama pablo).
 * Si el catálogo no responde o no tiene la propiedad, regresa vacío para que
 * el chat siga funcionando sin esos datos.
 */
@Component
public class CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);

    private final RestClient restClient;

    public CatalogoClient(@Value("${catalogo.url}") String urlCatalogo) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(urlCatalogo)
                .requestFactory(fabrica)
                .build();
    }

    public Optional<PropiedadCatalogo> buscarPorClave(String clave) {
        try {
            PropiedadCatalogo propiedad = restClient.get()
                    .uri("/api/propiedades/clave/{clave}", clave)
                    .retrieve()
                    .body(PropiedadCatalogo.class);

            return Optional.ofNullable(propiedad);

        } catch (HttpClientErrorException.NotFound exception) {
            log.warn("El catálogo no tiene la propiedad con clave {}", clave);
            return Optional.empty();

        } catch (RestClientException exception) {
            log.warn("No fue posible consultar el catálogo: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}
