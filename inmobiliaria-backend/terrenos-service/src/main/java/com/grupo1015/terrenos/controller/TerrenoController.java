package com.grupo1015.terrenos.controller;

import com.grupo1015.terrenos.model.Estatus;
import com.grupo1015.terrenos.model.Terreno;
import com.grupo1015.terrenos.service.TerrenoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CONTROLADOR: recibe las peticiones HTTP (Postman o la página web) y responde JSON.
 * En un microservicio la "vista" es ese JSON; la página estática lo pintará con fetch().
 */
@RestController
@RequestMapping("/api/terrenos")
@CrossOrigin(origins = "*") // permite que la página estática llame a la API desde el navegador
public class TerrenoController {

    private final TerrenoService service;

    public TerrenoController(TerrenoService service) {
        this.service = service;
    }

    // GET /api/terrenos            -> todos
    // GET /api/terrenos?estatus=DISPONIBLE&ubicacion=tlajomulco -> filtrados
    @GetMapping
    public List<Terreno> listar(@RequestParam(required = false) Estatus estatus,
                        @RequestParam(required = false) String ubicacion) {
        return service.listar(estatus, ubicacion);
    }

    @GetMapping("/{id}")
    public Terreno buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/clave/{clave}")
    public Terreno buscarPorClave(@PathVariable String clave) {
        return service.buscarPorClave(clave);
    }

    @PostMapping
    public ResponseEntity<Terreno> crear(@Valid @RequestBody Terreno terreno) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(terreno));
    }

    @PutMapping("/{id}")
    public Terreno actualizar(@PathVariable Long id, @Valid @RequestBody Terreno terreno) {
        return service.actualizar(id, terreno);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
