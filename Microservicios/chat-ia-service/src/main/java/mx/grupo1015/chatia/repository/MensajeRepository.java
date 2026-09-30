package mx.grupo1015.chatia.repository;

import mx.grupo1015.chatia.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeRepository
        extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByConversacion_IdOrderByFechaCreacionAsc(
            Long conversacionId
    );

    long countByConversacion_Id(Long conversacionId);
}