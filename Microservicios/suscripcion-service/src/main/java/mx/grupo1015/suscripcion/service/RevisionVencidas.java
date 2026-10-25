package mx.grupo1015.suscripcion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que revisa las suscripciones vencidas.
 */
@Component
public class RevisionVencidas {

    private static final Logger log = LoggerFactory.getLogger(RevisionVencidas.class);

    private final SuscripcionService suscripcionService;

    public RevisionVencidas(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @Scheduled(
            fixedDelayString = "${suscripciones.revision-vencidas-ms}",
            initialDelayString = "${suscripciones.revision-vencidas-ms}"
    )
    public void revisar() {
        int vencidas = suscripcionService.vencerSuscripciones();

        if (vencidas > 0) {
            log.info("Se vencieron {} suscripciones", vencidas);
        }
    }
}
