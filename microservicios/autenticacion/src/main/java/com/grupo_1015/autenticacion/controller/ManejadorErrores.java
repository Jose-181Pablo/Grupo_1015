package com.grupo_1015.autenticacion.controller;

import com.grupo_1015.autenticacion.service.AccesoDenegadoException;
import com.grupo_1015.autenticacion.service.CorreoYaRegistradoException;
import com.grupo_1015.autenticacion.service.CredencialesInvalidasException;
import com.grupo_1015.autenticacion.service.CuentaNoEncontradaException;
import com.grupo_1015.autenticacion.service.ServicioUsuarioNoDisponibleException;
import com.grupo_1015.autenticacion.service.TokenInvalidoException;
import com.grupo_1015.autenticacion.service.UltimoAdminException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Convierte los errores de TODOS los controladores (y del AdminInterceptor)
// en respuestas HTTP con un JSON {"error": "mensaje"}.
@RestControllerAdvice
public class ManejadorErrores {

    // Faltan datos o son invalidos -> 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e);
    }

    // Login incorrecto, sin token o token invalido -> 401 Unauthorized
    @ExceptionHandler({CredencialesInvalidasException.class, TokenInvalidoException.class})
    public ResponseEntity<Map<String, String>> noAutorizado(RuntimeException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, e);
    }

    // Token valido pero sin permiso (no es ADMIN) -> 403 Forbidden
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> sinPermiso(AccesoDenegadoException e) {
        return respuesta(HttpStatus.FORBIDDEN, e);
    }

    // La cuenta no existe -> 404 Not Found
    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> noEncontrada(CuentaNoEncontradaException e) {
        return respuesta(HttpStatus.NOT_FOUND, e);
    }

    // Correo repetido o quitar el ultimo ADMIN -> 409 Conflict
    @ExceptionHandler({CorreoYaRegistradoException.class, UltimoAdminException.class})
    public ResponseEntity<Map<String, String>> conflicto(RuntimeException e) {
        return respuesta(HttpStatus.CONFLICT, e);
    }

    // El microservicio usuario no responde -> 503 Service Unavailable
    @ExceptionHandler(ServicioUsuarioNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> usuarioNoDisponible(ServicioUsuarioNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e);
    }

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, RuntimeException e) {
        return ResponseEntity.status(estado).body(Map.of("error", e.getMessage()));
    }
}
