package com.grupo1015.microserviciocitas.repository;

import com.grupo1015.microserviciocitas.model.Cita;
import com.grupo1015.microserviciocitas.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByEstado(EstadoCita estado);
}