package com.grupo_1015.autenticacion.dto;

import com.grupo_1015.autenticacion.model.Plan;
import com.grupo_1015.autenticacion.model.Rol;

// DTO: lo que se responde al registrarse. La contrasena NUNCA se devuelve.
public record RegistroResponse(Long id, String correo, Long usuarioId, Rol rol, Plan plan) {
}
