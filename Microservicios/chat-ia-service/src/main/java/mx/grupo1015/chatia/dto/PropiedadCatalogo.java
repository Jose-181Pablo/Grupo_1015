package mx.grupo1015.chatia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Datos de una propiedad tal como los devuelve el microservicio de catálogo
 * (inmobiliaria-service): GET /api/propiedades/clave/{clave}.
 * Solo se declaran los campos que necesita el asistente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PropiedadCatalogo(
        String clave,
        String nombre,
        String categoria,
        String ubicacion,
        String descripcion,
        BigDecimal precio,
        BigDecimal terrenoM2,
        BigDecimal construccionM2,
        String distribucion,
        String amenidades,
        String estatus
) {
}
