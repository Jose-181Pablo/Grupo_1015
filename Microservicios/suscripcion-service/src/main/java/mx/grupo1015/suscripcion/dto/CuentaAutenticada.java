package mx.grupo1015.suscripcion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de GET /api/auth/validar del microservicio de autenticación.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CuentaAutenticada(
        Long cuentaId,
        String correo,
        Long usuarioId,
        String rol,
        String plan,
        boolean accesoPremium
) {
}
