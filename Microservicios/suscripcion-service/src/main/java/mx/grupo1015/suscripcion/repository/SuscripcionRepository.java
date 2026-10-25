package mx.grupo1015.suscripcion.repository;

import mx.grupo1015.suscripcion.model.EstadoSuscripcion;
import mx.grupo1015.suscripcion.model.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository
        extends JpaRepository<Suscripcion, Long> {

    List<Suscripcion> findByCuentaIdOrderByFechaCreacionDesc(Long cuentaId);

    Optional<Suscripcion> findFirstByCuentaIdAndEstadoOrderByFechaFinDesc(
            Long cuentaId,
            EstadoSuscripcion estado
    );

    List<Suscripcion> findByEstadoAndFechaFinBefore(
            EstadoSuscripcion estado,
            LocalDateTime fecha
    );
}
