package mx.grupo1015.suscripcion.dto;

import java.math.BigDecimal;

public record PlanResponse(
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionDias
) {
}
