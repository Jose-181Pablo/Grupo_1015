package com.grupo_1015.autenticacion.service;

// Error: se intenta quitar el rol al unico ADMIN que queda -> 409 Conflict.
// Si se permitiera, nadie podria administrar el sistema.
public class UltimoAdminException extends RuntimeException {

    public UltimoAdminException() {
        super("No se puede quitar el rol al ultimo ADMIN del sistema");
    }
}
