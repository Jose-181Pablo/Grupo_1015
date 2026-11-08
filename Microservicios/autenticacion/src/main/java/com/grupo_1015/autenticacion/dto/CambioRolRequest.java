package com.grupo_1015.autenticacion.dto;

import com.grupo_1015.autenticacion.model.Rol;

// DTO: cuerpo de PUT /api/auth/admin/cuentas/{id}/rol   ->  {"rol": "ADMIN"}
public record CambioRolRequest(Rol rol) {
}
