package mx.grupo1015.chatia.dto;

public record ChatResponse(
        Long conversacionId,
        MensajeResponse mensajeUsuario,
        MensajeResponse respuestaAsistente
) {
}