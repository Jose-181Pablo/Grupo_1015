package com.grupo1015.microservicioofertas.controller;

import com.grupo1015.microservicioofertas.model.Oferta;
import com.grupo1015.microservicioofertas.model.TipoPublicacion;
import com.grupo1015.microservicioofertas.service.OfertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ofertas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OfertaController {

    private final OfertaService service;

    @GetMapping
    public List<Oferta> listar(@RequestParam(required = false) TipoPublicacion tipo,
                               @RequestParam(required = false) String propiedad) {
        return service.listar(tipo, propiedad);
    }

    @GetMapping("/{id}")
    public Oferta obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Oferta> crear(@Valid @RequestBody Oferta oferta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(oferta));
    }

    @PutMapping("/{id}")
    public Oferta actualizar(@PathVariable Long id, @Valid @RequestBody Oferta oferta) {
        return service.actualizar(id, oferta);
    }

    @PatchMapping("/{id}/desactivar")
    public Oferta desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        service.borrar(id);
        return ResponseEntity.noContent().build();
    }
}