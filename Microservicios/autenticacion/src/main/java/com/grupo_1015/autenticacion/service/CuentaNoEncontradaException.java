package com.grupo_1015.autenticacion.service;

// Error: no existe una cuenta con ese id -> 404 Not Found.
public class CuentaNoEncontradaException extends RuntimeException {

    public CuentaNoEncontradaException(Long id) {
        super("No existe la cuenta con id " + id);
    }
}
