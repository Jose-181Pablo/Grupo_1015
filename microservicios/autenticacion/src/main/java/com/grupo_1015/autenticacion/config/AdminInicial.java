package com.grupo_1015.autenticacion.config;

import com.grupo_1015.autenticacion.dto.RegistroRequest;
import com.grupo_1015.autenticacion.model.Credencial;
import com.grupo_1015.autenticacion.model.Plan;
import com.grupo_1015.autenticacion.model.Rol;
import com.grupo_1015.autenticacion.repository.CredencialRepository;
import com.grupo_1015.autenticacion.service.AuthService;
import com.grupo_1015.autenticacion.service.ServicioUsuarioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Se ejecuta una vez al arrancar el microservicio.
// Si no existe ningun ADMIN, crea uno con ADMIN_CORREO y ADMIN_PASSWORD del .env
// (con plan PREMIUM). Asi el primer administrador nunca queda escrito en el codigo.
@Component
public class AdminInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final CredencialRepository credencialRepository;
    private final AuthService authService;
    private final String adminCorreo;
    private final String adminPassword;

    public AdminInicial(CredencialRepository credencialRepository, AuthService authService,
                        @Value("${admin.correo}") String adminCorreo,
                        @Value("${admin.password}") String adminPassword) {
        this.credencialRepository = credencialRepository;
        this.authService = authService;
        this.adminCorreo = adminCorreo.trim().toLowerCase();
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (credencialRepository.countByRol(Rol.ADMIN) > 0) {
            return; // ya hay al menos un ADMIN
        }

        // Si la cuenta no existe, se registra igual que cualquier usuario
        // (asi tambien se crea su perfil en el microservicio usuario)
        if (!credencialRepository.existsByCorreo(adminCorreo)) {
            try {
                authService.registrar(new RegistroRequest("Administrador", "Sistema", adminCorreo, adminPassword));
            } catch (ServicioUsuarioNoDisponibleException e) {
                log.warn("No se pudo crear el ADMIN inicial porque el microservicio usuario no responde. "
                        + "Se intentara de nuevo en el proximo arranque.");
                return;
            }
        }

        // Se convierte la cuenta en ADMIN con acceso PREMIUM
        Credencial admin = credencialRepository.findByCorreo(adminCorreo).orElseThrow();
        admin.setRol(Rol.ADMIN);
        admin.setPlan(Plan.PREMIUM);
        credencialRepository.save(admin);
        log.info("ADMIN inicial listo: {}", adminCorreo);
    }
}
