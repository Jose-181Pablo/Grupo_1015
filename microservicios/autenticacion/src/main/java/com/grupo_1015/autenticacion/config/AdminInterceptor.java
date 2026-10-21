package com.grupo_1015.autenticacion.config;

import com.grupo_1015.autenticacion.dto.ValidacionResponse;
import com.grupo_1015.autenticacion.model.Rol;
import com.grupo_1015.autenticacion.service.AccesoDenegadoException;
import com.grupo_1015.autenticacion.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Interceptor: se ejecuta ANTES de cada peticion a /api/auth/admin/** (ver WebConfig).
//  - Sin token o token invalido -> AuthService lanza TokenInvalidoException -> 401
//  - Token valido pero rol distinto de ADMIN -> AccesoDenegadoException -> 403
//  - Si es ADMIN -> devuelve true y la peticion llega al AdminController
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AdminInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ValidacionResponse cuenta = authService.validar(request.getHeader("Authorization"));
        if (cuenta.rol() != Rol.ADMIN) {
            throw new AccesoDenegadoException();
        }
        return true;
    }
}
