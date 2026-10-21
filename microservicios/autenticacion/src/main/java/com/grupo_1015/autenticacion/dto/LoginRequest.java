package com.grupo_1015.autenticacion.dto;

// DTO: datos que llegan en el cuerpo de POST /api/auth/login.
public record LoginRequest(String correo, String password) {
}
