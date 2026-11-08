package com.grupo_1015.autenticacion.dto;

// DTO: respuesta del login. El cliente debe enviar el token en las siguientes
// peticiones con el encabezado:  Authorization: Bearer <token>
public record TokenResponse(String token, String tipo, long expiraEnMinutos) {
}
