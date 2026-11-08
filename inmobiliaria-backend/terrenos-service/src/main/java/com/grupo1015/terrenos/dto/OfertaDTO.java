package com.grupo1015.terrenos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO: copia de la oferta que responde el microservicio de ofertas.
 * Solo tiene los campos que necesitamos; los campos que no conocemos se ignoran.
 */
public record OfertaDTO(
        Long id,
        String propiedad,
        String titulo,
        String descripcion,
        String tipo,
        BigDecimal precioOriginal,
        BigDecimal precioFinal,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activa
) {
}
