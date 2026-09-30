package mx.grupo1015.chatia.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversacionResponse(
        Long id,
        String usuarioId,
        String propiedadId,
        String titulo,
        LocalDateTime fechaCreacion,
        LocalDateTime ultimaActualizacion,
        List<MensajeResponse> mensajes
) {
}