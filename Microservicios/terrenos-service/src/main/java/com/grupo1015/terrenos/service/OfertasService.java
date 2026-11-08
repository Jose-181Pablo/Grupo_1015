package com.grupo1015.terrenos.service;

import com.grupo1015.terrenos.dto.OfertaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * SERVICIO: consulta el microservicio de ofertas (de Génesis) para saber
 * el precio de oferta o remate de una casa.
 * Si el otro servicio falla, no revienta: devuelve vacío y deja un aviso en la consola.
 */
@Service
public class OfertasService {

    private static final Logger log = LoggerFactory.getLogger(OfertasService.class);

    private final RestClient restClient;
    private final String ofertasUrl;

    public OfertasService(RestClient ofertasRestClient,
                          @Value("${ofertas.url}") String ofertasUrl) {
        this.restClient = ofertasRestClient;
        this.ofertasUrl = ofertasUrl;
    }

    /**
     * Devuelve el precio final de la oferta activa de una casa, o vacío si no hay
     * oferta o si el microservicio de ofertas no responde.
     *
     * @param propiedad la clave de la casa, p. ej. "hacienda"
     */
    public Optional<BigDecimal> precioFinalDe(String propiedad) {
        String url = UriComponentsBuilder.fromUriString(ofertasUrl)
                .queryParam("propiedad", propiedad)
                .toUriString();
        try {
            OfertaDTO[] ofertas = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OfertaDTO[].class);
            if (ofertas == null) {
                return Optional.empty();
            }
            return Arrays.stream(ofertas)
                    .filter(OfertaDTO::activa)
                    .map(OfertaDTO::precioFinal)
                    .findFirst();
        } catch (RestClientException e) {
            log.warn("No se pudo consultar el servicio de ofertas ({}): {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
