package com.grupo_1015.autenticacion.controller;

import com.grupo_1015.autenticacion.dto.CambioPlanRequest;
import com.grupo_1015.autenticacion.dto.CambioRolRequest;
import com.grupo_1015.autenticacion.dto.CuentaResponse;
import com.grupo_1015.autenticacion.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Controlador de administracion. Todas sus rutas estan protegidas por AdminInterceptor:
// sin token -> 401, con token pero sin rol ADMIN -> 403.
@RestController
@RequestMapping("/api/auth/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/auth/admin/cuentas
    @GetMapping("/cuentas")
    public List<CuentaResponse> listarCuentas() {
        return adminService.listarCuentas();
    }

    // PUT /api/auth/admin/cuentas/{id}/plan   cuerpo: {"plan": "PREMIUM"}
    @PutMapping("/cuentas/{id}/plan")
    public CuentaResponse cambiarPlan(@PathVariable Long id, @RequestBody CambioPlanRequest datos) {
        return adminService.cambiarPlan(id, datos.plan());
    }

    // PUT /api/auth/admin/cuentas/{id}/rol   cuerpo: {"rol": "ADMIN"}
    @PutMapping("/cuentas/{id}/rol")
    public CuentaResponse cambiarRol(@PathVariable Long id, @RequestBody CambioRolRequest datos) {
        return adminService.cambiarRol(id, datos.rol());
    }
}
