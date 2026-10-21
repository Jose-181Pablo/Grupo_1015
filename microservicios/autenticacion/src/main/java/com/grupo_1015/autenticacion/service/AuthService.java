package com.grupo_1015.autenticacion.service;

import com.grupo_1015.autenticacion.client.UsuarioClient;
import com.grupo_1015.autenticacion.dto.RegistroRequest;
import com.grupo_1015.autenticacion.dto.RegistroResponse;
import com.grupo_1015.autenticacion.model.Credencial;
import com.grupo_1015.autenticacion.repository.CredencialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

// Servicio: logica de autenticacion (registro; despues login y validacion).
@Service
public class AuthService {

    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioClient usuarioClient;

    public AuthService(CredencialRepository credencialRepository, PasswordEncoder passwordEncoder,
                       UsuarioClient usuarioClient) {
        this.credencialRepository = credencialRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioClient = usuarioClient;
    }

    public RegistroResponse registrar(RegistroRequest datos) {
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

        return new RegistroResponse(guardada.getId(), guardada.getCorreo(), guardada.getUsuarioId(),
                guardada.getRol(), guardada.getPlan());
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
