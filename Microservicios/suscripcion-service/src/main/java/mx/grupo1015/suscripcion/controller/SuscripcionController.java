package mx.grupo1015.suscripcion.controller;

import jakarta.validation.Valid;
import mx.grupo1015.suscripcion.dto.ContratarSuscripcionRequest;
import mx.grupo1015.suscripcion.dto.SuscripcionResponse;
import mx.grupo1015.suscripcion.service.SuscripcionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Todas las rutas usan el token de autenticación: Authorization: Bearer <token>
 */
@RestController
@RequestMapping("/api/v1/suscripciones")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    public SuscripcionController(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @PostMapping
    public ResponseEntity<SuscripcionResponse> contratar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ContratarSuscripcionRequest request
    ) {
        SuscripcionResponse suscripcion =
                suscripcionService.contratar(authorization, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(suscripcion);
    }

    @GetMapping("/actual")
    public ResponseEntity<SuscripcionResponse> obtenerActual(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(
                suscripcionService.obtenerActual(authorization)
        );
    }

    @GetMapping
    public ResponseEntity<List<SuscripcionResponse>> obtenerHistorial(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(
                suscripcionService.obtenerHistorial(authorization)
        );
    }

    @PostMapping("/actual/cancelar")
    public ResponseEntity<SuscripcionResponse> cancelar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        return ResponseEntity.ok(
                suscripcionService.cancelar(authorization)
        );
    }
}
