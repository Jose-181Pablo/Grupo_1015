package com.grupo_1015.autenticacion.controller;

import com.grupo_1015.autenticacion.dto.RegistroRequest;
import com.grupo_1015.autenticacion.dto.RegistroResponse;
import com.grupo_1015.autenticacion.service.AuthService;
import com.grupo_1015.autenticacion.service.CorreoYaRegistradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Controlador: recibe las peticiones HTTP de autenticacion y responde en JSON.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/registro
    @PostMapping("/registro")
    public ResponseEntity<RegistroResponse> registrar(@RequestBody RegistroRequest datos) {
        RegistroResponse respuesta = authService.registrar(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // Correo repetido -> 409 Conflict
    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<Map<String, String>> correoRepetido(CorreoYaRegistradoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    // Faltan datos -> 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
