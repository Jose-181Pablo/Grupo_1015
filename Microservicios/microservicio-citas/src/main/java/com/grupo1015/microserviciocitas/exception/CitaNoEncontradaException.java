package com.grupo1015.microserviciocitas.exception;

public class CitaNoEncontradaException extends RuntimeException {
    public CitaNoEncontradaException(Long id) {
        super("No existe la cita con id " + id);
    }
}