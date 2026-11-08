package com.grupo1015.microservicioofertas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Lo que responde GET /api/auth/validar (solo los campos que se usan aquí). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CuentaValidada(Long cuentaId, String correo, String rol) {

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
