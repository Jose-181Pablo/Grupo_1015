package mx.grupo1015.suscripcion.controller;

import mx.grupo1015.suscripcion.dto.PlanResponse;
import mx.grupo1015.suscripcion.service.SuscripcionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planes")
public class PlanController {

    private final SuscripcionService suscripcionService;

    public PlanController(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> obtenerPlanes() {
        return ResponseEntity.ok(
                suscripcionService.obtenerPlanes()
        );
    }
}
