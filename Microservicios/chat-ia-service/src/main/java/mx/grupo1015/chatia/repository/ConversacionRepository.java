package mx.grupo1015.chatia.repository;

import mx.grupo1015.chatia.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository
        extends JpaRepository<Conversacion, Long> {

    List<Conversacion> findByUsuarioIdOrderByUltimaActualizacionDesc(
            String usuarioId
    );

    List<Conversacion> findByPropiedadIdOrderByUltimaActualizacionDesc(
            String propiedadId
    );

    Optional<Conversacion> findByIdAndUsuarioId(
            Long id,
            String usuarioId
    );
}