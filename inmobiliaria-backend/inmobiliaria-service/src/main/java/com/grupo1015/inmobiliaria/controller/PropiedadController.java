package com.grupo1015.inmobiliaria.controller;

import com.grupo1015.inmobiliaria.model.Categoria;
import com.grupo1015.inmobiliaria.model.Propiedad;
import com.grupo1015.inmobiliaria.service.PropiedadService;
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
@RequestMapping("/api/propiedades")
@CrossOrigin(origins = "*") // permite que la página estática llame a la API desde el navegador
public class PropiedadController {

    private final PropiedadService service;

    public PropiedadController(PropiedadService service) {
        this.service = service;
    }

    // GET /api/propiedades            -> todas
    // GET /api/propiedades?categoria=CASA&ubicacion=tlajomulco -> filtradas
    @GetMapping
    public List<Propiedad> listar(@RequestParam(required = false) Categoria categoria,
                                  @RequestParam(required = false) String ubicacion) {
        return service.listar(categoria, ubicacion);
    }

    @GetMapping("/{id}")
    public Propiedad buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/clave/{clave}")
    public Propiedad buscarPorClave(@PathVariable String clave) {
        return service.buscarPorClave(clave);
    }

    @PostMapping
    public ResponseEntity<Propiedad> crear(@Valid @RequestBody Propiedad propiedad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(propiedad));
    }

    @PutMapping("/{id}")
    public Propiedad actualizar(@PathVariable Long id, @Valid @RequestBody Propiedad propiedad) {
        return service.actualizar(id, propiedad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
