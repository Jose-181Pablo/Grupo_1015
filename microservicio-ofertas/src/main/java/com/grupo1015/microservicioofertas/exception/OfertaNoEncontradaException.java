package com.grupo1015.microservicioofertas.exception;

public class OfertaNoEncontradaException extends RuntimeException {
    public OfertaNoEncontradaException(Long id) {
        super("No existe la oferta con id " + id);
    }
}