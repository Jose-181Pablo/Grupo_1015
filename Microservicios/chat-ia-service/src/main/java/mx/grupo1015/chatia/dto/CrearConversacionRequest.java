package mx.grupo1015.chatia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearConversacionRequest(

        @Size(max = 100, message = "El usuarioId no puede superar 100 caracteres")
        String usuarioId,

        @NotBlank(message = "La propiedad es obligatoria")
        @Size(max = 100, message = "El propiedadId no puede superar 100 caracteres")
        String propiedadId,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo
) {
}