package com.grupo_1015.autenticacion.dto;

import com.grupo_1015.autenticacion.model.Plan;
import com.grupo_1015.autenticacion.model.Rol;

// DTO: respuesta de GET /api/auth/validar. La usan otros microservicios
// (por ejemplo el chat con IA o pago) para saber quien hace la peticion y que puede hacer.
// cuentaId es el id en autenticacion (el que usan los endpoints de admin);
// usuarioId es el id del perfil en el microservicio usuario.
// accesoPremium es true si el plan es PREMIUM o si es ADMIN.
public record ValidacionResponse(Long cuentaId, String correo, Long usuarioId, Rol rol, Plan plan,
                                 boolean accesoPremium) {
}
