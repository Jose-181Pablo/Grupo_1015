package com.grupo_1015.autenticacion.service;

import com.grupo_1015.autenticacion.dto.CuentaResponse;
import com.grupo_1015.autenticacion.model.Credencial;
import com.grupo_1015.autenticacion.model.Plan;
import com.grupo_1015.autenticacion.model.Rol;
import com.grupo_1015.autenticacion.repository.CredencialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Servicio: funciones que solo puede usar un ADMIN.
// (El AdminInterceptor ya comprobo que quien llama es ADMIN.)
@Service
public class AdminService {

    private final CredencialRepository credencialRepository;

    public AdminService(CredencialRepository credencialRepository) {
        this.credencialRepository = credencialRepository;
    }

    public List<CuentaResponse> listarCuentas() {
        return credencialRepository.findAll().stream()
                .map(CuentaResponse::de)
                .toList();
    }

    // Lo usara tambien el microservicio de pago para activar PREMIUM
    public CuentaResponse cambiarPlan(Long id, Plan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("El plan es obligatorio (ESTANDAR o PREMIUM)");
        }
        Credencial credencial = buscar(id);
        credencial.setPlan(plan);
        return CuentaResponse.de(credencialRepository.save(credencial));
    }

    public CuentaResponse cambiarRol(Long id, Rol rol) {
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio (ADMIN o USUARIO)");
        }
        Credencial credencial = buscar(id);

        // No se permite dejar el sistema sin ningun ADMIN
        boolean quitaAdmin = credencial.getRol() == Rol.ADMIN && rol == Rol.USUARIO;
        if (quitaAdmin && credencialRepository.countByRol(Rol.ADMIN) <= 1) {
            throw new UltimoAdminException();
        }

        credencial.setRol(rol);
        return CuentaResponse.de(credencialRepository.save(credencial));
    }

    private Credencial buscar(Long id) {
        return credencialRepository.findById(id)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));
    }
}
