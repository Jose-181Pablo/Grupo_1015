package com.grupo_1015.autenticacion.controller;

import com.grupo_1015.autenticacion.dto.CuentaResponse;
import com.grupo_1015.autenticacion.dto.LoginRequest;
import com.grupo_1015.autenticacion.dto.RegistroRequest;
import com.grupo_1015.autenticacion.dto.TokenResponse;
import com.grupo_1015.autenticacion.dto.ValidacionResponse;
import com.grupo_1015.autenticacion.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador: endpoints publicos de autenticacion.
// Los errores (400, 401, 409, 503) se convierten en ManejadorErrores.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/registro
    @PostMapping("/registro")
    public ResponseEntity<CuentaResponse> registrar(@RequestBody RegistroRequest datos) {
        CuentaResponse respuesta = authService.registrar(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest datos) {
        return authService.login(datos);
    }

    // GET /api/auth/validar   (encabezado: Authorization: Bearer <token>)
    @GetMapping("/validar")
    public ValidacionResponse validar(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.validar(authorization);
    }
}
