package mx.grupo1015.chatia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeRequest(

        @NotBlank(message = "La pregunta no puede estar vacía")
        @Size(
                max = 2000,
                message = "La pregunta no puede superar 2000 caracteres"
        )
        String pregunta
) {
}