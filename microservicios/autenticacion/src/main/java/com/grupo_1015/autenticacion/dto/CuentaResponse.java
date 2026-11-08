package com.grupo_1015.autenticacion.dto;

import com.grupo_1015.autenticacion.model.Credencial;
import com.grupo_1015.autenticacion.model.Plan;
import com.grupo_1015.autenticacion.model.Rol;

// DTO: datos publicos de una cuenta (registro y administracion).
// La contrasena NUNCA se devuelve.
public record CuentaResponse(Long id, String correo, Long usuarioId, Rol rol, Plan plan) {

    // Convierte la entidad en DTO (asi no se repite este codigo en cada servicio)
    public static CuentaResponse de(Credencial credencial) {
        return new CuentaResponse(credencial.getId(), credencial.getCorreo(), credencial.getUsuarioId(),
                credencial.getRol(), credencial.getPlan());
    }
}
