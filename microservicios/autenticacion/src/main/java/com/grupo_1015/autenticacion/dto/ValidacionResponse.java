package com.grupo_1015.autenticacion.dto;

import com.grupo_1015.autenticacion.model.Plan;
import com.grupo_1015.autenticacion.model.Rol;

// DTO: respuesta de GET /api/auth/validar. La usan otros microservicios
// (por ejemplo el chat con IA) para saber quien hace la peticion y que puede hacer.
// accesoPremium es true si el plan es PREMIUM o si es ADMIN.
public record ValidacionResponse(String correo, Long usuarioId, Rol rol, Plan plan, boolean accesoPremium) {
}
