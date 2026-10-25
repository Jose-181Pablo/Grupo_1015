package mx.grupo1015.suscripcion.repository;

import mx.grupo1015.suscripcion.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository
        extends JpaRepository<Plan, Long> {

    List<Plan> findByActivoTrueOrderByPrecioAsc();

    Optional<Plan> findByCodigoAndActivoTrue(String codigo);
}
