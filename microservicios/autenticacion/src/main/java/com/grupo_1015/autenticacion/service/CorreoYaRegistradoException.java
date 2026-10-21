package com.grupo_1015.autenticacion.service;

// Error que se lanza cuando alguien intenta registrarse con un correo que ya existe.
// El controlador lo convierte en una respuesta 409 Conflict.
public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException(String correo) {
        super("El correo " + correo + " ya esta registrado");
    }
}
