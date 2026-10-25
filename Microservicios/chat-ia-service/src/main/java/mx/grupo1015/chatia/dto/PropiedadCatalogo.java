package mx.grupo1015.chatia.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Datos de una propiedad tal como los devuelven los microservicios de catálogo (rama pablo):
 * inmuebles-service (GET /api/inmuebles/clave/{clave}) y terrenos-service (GET /api/terrenos/clave/{clave}).
 * Solo se declaran los campos que necesita el asistente; la categoría la pone CatalogoClient.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PropiedadCatalogo(
        String clave,
        String nombre,
        String categoria,
        String ubicacion,
        String descripcion,
        BigDecimal precio,
        // En terrenos-service el campo se llama superficieM2
        @JsonAlias("superficieM2")
        BigDecimal terrenoM2,
        BigDecimal construccionM2,
        String distribucion,
        String medidas,
        String usoSuelo,
        String amenidades,
        String estatus
) {

    public PropiedadCatalogo conCategoria(String nuevaCategoria) {
        return new PropiedadCatalogo(
                clave, nombre, nuevaCategoria, ubicacion, descripcion, precio,
                terrenoM2, construccionM2, distribucion, medidas, usoSuelo,
                amenidades, estatus
        );
    }
}
