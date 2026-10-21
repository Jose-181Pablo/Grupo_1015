package com.grupo_1015.autenticacion.dto;

import com.grupo_1015.autenticacion.model.Plan;

// DTO: cuerpo de PUT /api/auth/admin/cuentas/{id}/plan   ->  {"plan": "PREMIUM"}
public record CambioPlanRequest(Plan plan) {
}
