package com.grupo_1015.autenticacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Configuracion: crea los objetos que Spring inyecta en otras clases.
@Configuration
public class SeguridadConfig {

    // BCrypt cifra las contrasenas de forma irreversible (no se pueden "descifrar").
    // Para el login solo se compara si la contrasena escrita coincide con la cifrada.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
