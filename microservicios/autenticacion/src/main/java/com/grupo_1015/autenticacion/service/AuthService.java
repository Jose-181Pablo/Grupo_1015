package com.grupo_1015.autenticacion.service;

import com.grupo_1015.autenticacion.dto.RegistroRequest;
import com.grupo_1015.autenticacion.dto.RegistroResponse;
import com.grupo_1015.autenticacion.model.Credencial;
import com.grupo_1015.autenticacion.repository.CredencialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Servicio: logica de autenticacion (registro; despues login y validacion).
@Service
public class AuthService {

    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(CredencialRepository credencialRepository, PasswordEncoder passwordEncoder) {
        this.credencialRepository = credencialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegistroResponse registrar(RegistroRequest datos) {
        if (estaVacio(datos.correo()) || estaVacio(datos.password())) {
            throw new IllegalArgumentException("El correo y la contrasena son obligatorios");
        }
        String correo = datos.correo().trim().toLowerCase();

        if (credencialRepository.existsByCorreo(correo)) {
            throw new CorreoYaRegistradoException(correo);
        }

        // Temporal (etapa A3): en la etapa A4 este id lo devolvera el microservicio usuario
        Long usuarioId = 0L;

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
