package mx.grupo1015.suscripcion.config;

import mx.grupo1015.suscripcion.model.Plan;
import mx.grupo1015.suscripcion.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Crea los planes de ejemplo la primera vez que arranca el microservicio.
 */
@Component
public class PlanesIniciales implements CommandLineRunner {

    private final PlanRepository planRepository;

    public PlanesIniciales(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) {
        if (planRepository.count() > 0) {
            return;
        }

        planRepository.saveAll(List.of(
                new Plan(
                        "PREMIUM_MENSUAL",
                        "Premium mensual",
                        "Acceso al chat con IA durante 30 días",
                        new BigDecimal("149.00"),
                        30
                ),
                new Plan(
                        "PREMIUM_ANUAL",
                        "Premium anual",
                        "Acceso al chat con IA durante 365 días",
                        new BigDecimal("1490.00"),
                        365
                )
        ));
    }
}
