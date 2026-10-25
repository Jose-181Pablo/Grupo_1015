package mx.grupo1015.suscripcion.dto;

import mx.grupo1015.suscripcion.model.EstadoSuscripcion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SuscripcionResponse(
        Long id,
        Long cuentaId,
        Long usuarioId,
        String correo,
        PlanResponse plan,
        EstadoSuscripcion estado,
        BigDecimal monto,
        String referenciaPago,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        LocalDateTime fechaCancelacion
) {
}
