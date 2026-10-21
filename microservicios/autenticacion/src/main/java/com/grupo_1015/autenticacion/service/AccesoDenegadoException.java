package com.grupo_1015.autenticacion.service;

// Error: el token es valido pero la cuenta no tiene permiso -> 403 Forbidden.
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super("No tienes permiso para realizar esta accion");
    }
}
