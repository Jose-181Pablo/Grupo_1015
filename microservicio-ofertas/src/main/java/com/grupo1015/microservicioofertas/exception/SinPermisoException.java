package com.grupo1015.microservicioofertas.exception;

public class SinPermisoException extends RuntimeException {
    public SinPermisoException(String mensaje) {
        super(mensaje);
    }
}
