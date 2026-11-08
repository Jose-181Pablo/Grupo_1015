package mx.grupo1015.chatia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Oferta o remate de una propiedad, tal como lo devuelve microservicio-ofertas (rama gene1):
 * GET /api/ofertas?propiedad={clave}.
 * Solo se declaran los campos que necesita el asistente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OfertaVigente(
        String titulo,
        String descripcion,
        // OFERTA o REMATE
        String tipo,
        BigDecimal precioOriginal,
        BigDecimal precioFinal,
        LocalDate fechaFin,
        boolean activa
) {

    /**
     * Está vigente si sigue activa y su fecha de fin no ha pasado.
     */
    public boolean vigenteEn(LocalDate hoy) {
        return activa && (fechaFin == null || !fechaFin.isBefore(hoy));
    }
}
