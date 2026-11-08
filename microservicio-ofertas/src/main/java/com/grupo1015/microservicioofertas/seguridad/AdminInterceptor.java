package com.grupo1015.microservicioofertas.seguridad;

import com.grupo1015.microservicioofertas.client.AutenticacionClient;
import com.grupo1015.microservicioofertas.client.CuentaValidada;
import com.grupo1015.microservicioofertas.exception.SinPermisoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Consultar ofertas es libre (la página, el chat y terrenos las leen).
 * Crear, editar, desactivar y borrar solo lo puede hacer una cuenta ADMIN:
 *   sin token o token inválido -> 401, token sin rol ADMIN -> 403,
 *   autenticación apagada -> 503.
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private static final Set<String> METODOS_LIBRES = Set.of("GET", "HEAD", "OPTIONS");

    private final AutenticacionClient autenticacionClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (METODOS_LIBRES.contains(request.getMethod())) {
            return true;
        }

        CuentaValidada cuenta = autenticacionClient.validar(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (!cuenta.esAdmin()) {
            throw new SinPermisoException("Solo los administradores pueden crear o modificar ofertas y remates.");
        }
        return true;
    }
}
