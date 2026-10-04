package com.grupo1015.microserviciocitas.service;

import com.grupo1015.microserviciocitas.exception.CitaNoEncontradaException;
import com.grupo1015.microserviciocitas.model.Cita;
import com.grupo1015.microserviciocitas.model.EstadoCita;
import com.grupo1015.microserviciocitas.repository.CitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository repo;

    public List<Cita> listar() {
        return repo.findAll();
    }

    public List<Cita> listarPorEstado(EstadoCita estado) {
        return repo.findByEstado(estado);
    }

    public Cita obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new CitaNoEncontradaException(id));
    }

    public Cita crear(Cita cita) {
        cita.setId(null);                      // evita que alguien sobrescriba una cita existente
        cita.setEstado(EstadoCita.PENDIENTE);  // toda cita nueva empieza pendiente
        return repo.save(cita);
    }

    public Cita cambiarEstado(Long id, EstadoCita estado) {
        Cita cita = obtener(id);
        cita.setEstado(estado);
        return repo.save(cita);
    }

    public void borrar(Long id) {
        if (!repo.existsById(id)) {
            throw new CitaNoEncontradaException(id);
        }
        repo.deleteById(id);
    }
}