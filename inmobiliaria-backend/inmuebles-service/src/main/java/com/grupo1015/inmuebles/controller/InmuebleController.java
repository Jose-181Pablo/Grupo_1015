package com.grupo1015.inmuebles.controller;

import com.grupo1015.inmuebles.model.Estatus;
import com.grupo1015.inmuebles.model.Inmueble;
import com.grupo1015.inmuebles.service.InmuebleService;
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
@RequestMapping("/api/inmuebles")
@CrossOrigin(origins = "*") // permite que la página estática llame a la API desde el navegador
public class InmuebleController {

    private final InmuebleService service;

    public InmuebleController(InmuebleService service) {
        this.service = service;
    }

    // GET /api/inmuebles            -> todos
    // GET /api/inmuebles?estatus=DISPONIBLE&ubicacion=tlajomulco -> filtrados
    @GetMapping
    public List<Inmueble> listar(@RequestParam(required = false) Estatus estatus,
                        @RequestParam(required = false) String ubicacion) {
        return service.listar(estatus, ubicacion);
    }

    @GetMapping("/{id}")
    public Inmueble buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/clave/{clave}")
    public Inmueble buscarPorClave(@PathVariable String clave) {
        return service.buscarPorClave(clave);
    }

    @PostMapping
    public ResponseEntity<Inmueble> crear(@Valid @RequestBody Inmueble inmueble) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(inmueble));
    }

    @PutMapping("/{id}")
    public Inmueble actualizar(@PathVariable Long id, @Valid @RequestBody Inmueble inmueble) {
        return service.actualizar(id, inmueble);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
