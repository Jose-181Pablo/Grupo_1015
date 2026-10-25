package mx.grupo1015.suscripcion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContratarSuscripcionRequest(

        @NotBlank(message = "El plan es obligatorio")
        @Size(max = 50, message = "El código del plan no puede superar 50 caracteres")
        String planCodigo
) {
}
