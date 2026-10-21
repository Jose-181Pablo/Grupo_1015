package com.grupo_1015.autenticacion.service;

// Error de validacion: no hay token, es falso, fue modificado o ya vencio -> 401 Unauthorized.
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("Token invalido o vencido, inicia sesion de nuevo");
    }
}
