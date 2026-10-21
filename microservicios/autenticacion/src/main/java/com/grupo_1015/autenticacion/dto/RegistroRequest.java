package com.grupo_1015.autenticacion.dto;

// DTO: datos que llegan en el cuerpo de POST /api/auth/registro.
// nombre y apellido se enviaran al microservicio usuario (etapa A4).
public record RegistroRequest(String nombre, String apellido, String correo, String password) {
}
