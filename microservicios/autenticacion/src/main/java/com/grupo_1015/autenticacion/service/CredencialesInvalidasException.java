package com.grupo_1015.autenticacion.service;

// Error del login: correo o contrasena incorrectos -> 401 Unauthorized.
// El mensaje es el mismo en ambos casos para no revelar que correos existen.
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contrasena incorrectos");
    }
}
