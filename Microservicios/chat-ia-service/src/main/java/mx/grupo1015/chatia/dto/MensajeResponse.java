package mx.grupo1015.chatia.dto;

import mx.grupo1015.chatia.model.Remitente;

import java.time.LocalDateTime;

public record MensajeResponse(
        Long id,
        Remitente remitente,
        String contenido,
        LocalDateTime fechaCreacion
) {
}