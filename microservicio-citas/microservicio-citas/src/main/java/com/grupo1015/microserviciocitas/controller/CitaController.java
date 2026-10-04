package com.grupo1015.microserviciocitas.controller;

import com.grupo1015.microserviciocitas.model.Cita;
import com.grupo1015.microserviciocitas.model.EstadoCita;
import com.grupo1015.microserviciocitas.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService service;

    @GetMapping
    public List<Cita> listar(@RequestParam(required = false) EstadoCita estado) {
        return estado == null ? service.listar() : service.listarPorEstado(estado);
    }

    @GetMapping("/{id}")
    public Cita obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Cita> crear(@Valid @RequestBody Cita cita) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(cita));
    }

    @PatchMapping("/{id}/estado")
    public Cita cambiarEstado(@PathVariable Long id, @RequestParam EstadoCita estado) {
        return service.cambiarEstado(id, estado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        service.borrar(id);
        return ResponseEntity.noContent().build();
    }
}