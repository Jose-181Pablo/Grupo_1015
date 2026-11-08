package com.grupo_1015.autenticacion.service;

import com.grupo_1015.autenticacion.client.UsuarioClient;
import com.grupo_1015.autenticacion.dto.LoginRequest;
import com.grupo_1015.autenticacion.dto.RegistroRequest;
import com.grupo_1015.autenticacion.dto.CuentaResponse;
import com.grupo_1015.autenticacion.dto.TokenResponse;
import com.grupo_1015.autenticacion.dto.ValidacionResponse;
import com.grupo_1015.autenticacion.model.Credencial;
import com.grupo_1015.autenticacion.model.Plan;
import com.grupo_1015.autenticacion.model.Rol;
import com.grupo_1015.autenticacion.repository.CredencialRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

// Servicio: logica de autenticacion (registro; despues login y validacion).
@Service
public class AuthService {

    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioClient usuarioClient;
    private final JwtService jwtService;

    public AuthService(CredencialRepository credencialRepository, PasswordEncoder passwordEncoder,
                       UsuarioClient usuarioClient, JwtService jwtService) {
        this.credencialRepository = credencialRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioClient = usuarioClient;
        this.jwtService = jwtService;
    }

    public CuentaResponse registrar(RegistroRequest datos) {
        if (estaVacio(datos.correo()) || estaVacio(datos.password())) {
            throw new IllegalArgumentException("El correo y la contrasena son obligatorios");
        }
        String correo = datos.correo().trim().toLowerCase();

        if (credencialRepository.existsByCorreo(correo)) {
            throw new CorreoYaRegistradoException(correo);
        }

        // Se le pide al microservicio usuario que cree el perfil (nombre, apellido, correo)
        Long usuarioId;
        try {
            usuarioId = usuarioClient.crearUsuario(datos.nombre(), datos.apellido(), correo);
        } catch (RestClientException e) {
            // usuario esta apagado o respondio con error
            throw new ServicioUsuarioNoDisponibleException();
        }

        // La contrasena se guarda cifrada con BCrypt; el rol y el plan quedan
        // con sus valores por defecto: USUARIO y ESTANDAR
        Credencial credencial = new Credencial(correo, passwordEncoder.encode(datos.password()), usuarioId);
        Credencial guardada = credencialRepository.save(credencial);

        return CuentaResponse.de(guardada);
    }

    public TokenResponse login(LoginRequest datos) {
        if (estaVacio(datos.correo()) || estaVacio(datos.password())) {
            throw new CredencialesInvalidasException();
        }
        String correo = datos.correo().trim().toLowerCase();

        // BCrypt no se descifra: matches() compara la contrasena escrita con la cifrada
        Credencial credencial = credencialRepository.findByCorreo(correo)
                .filter(c -> passwordEncoder.matches(datos.password(), c.getPassword()))
                .orElseThrow(CredencialesInvalidasException::new);

        String token = jwtService.generarToken(credencial);
        return new TokenResponse(token, "Bearer", jwtService.getExpiracionMinutos());
    }

    // Recibe el encabezado "Authorization: Bearer <token>" y dice quien es y que puede hacer.
    // El rol y el plan se leen de la base de datos en este momento (no del token), asi un
    // cambio de plan se nota de inmediato aunque el token se haya creado antes.
    public ValidacionResponse validar(String encabezadoAuthorization) {
        if (encabezadoAuthorization == null || !encabezadoAuthorization.startsWith("Bearer ")) {
            throw new TokenInvalidoException();
        }
        String token = encabezadoAuthorization.substring("Bearer ".length()).trim();

        String correo;
        try {
            correo = jwtService.obtenerCorreo(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException(); // falso, modificado o vencido
        }

        Credencial credencial = credencialRepository.findByCorreo(correo)
                .orElseThrow(TokenInvalidoException::new); // la cuenta ya no existe

        boolean accesoPremium = credencial.getPlan() == Plan.PREMIUM || credencial.getRol() == Rol.ADMIN;
        return new ValidacionResponse(credencial.getId(), credencial.getCorreo(), credencial.getUsuarioId(),
                credencial.getRol(), credencial.getPlan(), accesoPremium);
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
