package com.grupo_1015.autenticacion.service;

// Error que se lanza cuando el microservicio usuario no responde.
// El controlador lo convierte en una respuesta 503 Service Unavailable.
public class ServicioUsuarioNoDisponibleException extends RuntimeException {

    public ServicioUsuarioNoDisponibleException() {
        super("El servicio de usuarios no esta disponible, intenta mas tarde");
    }
}
