package com.grupo1015.inmuebles.service;

import com.grupo1015.inmuebles.exception.RecursoDuplicadoException;
import com.grupo1015.inmuebles.exception.RecursoNoEncontradoException;
import com.grupo1015.inmuebles.model.Estatus;
import com.grupo1015.inmuebles.model.Inmueble;
import com.grupo1015.inmuebles.repository.InmuebleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SERVICIO: aquí van las reglas de negocio.
 * El controlador nunca habla directo con el repositorio, siempre pasa por aquí.
 */
@Service
public class InmuebleService {

    private final InmuebleRepository repository;

    public InmuebleService(InmuebleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Inmueble> listar(Estatus estatus, String ubicacion) {
        boolean hayUbicacion = ubicacion != null && !ubicacion.isBlank();
        if (estatus != null && hayUbicacion) {
            return repository.findByEstatusAndUbicacionContainingIgnoreCase(estatus, ubicacion);
        }
        if (estatus != null) {
            return repository.findByEstatus(estatus);
        }
        if (hayUbicacion) {
            return repository.findByUbicacionContainingIgnoreCase(ubicacion);
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Inmueble buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el inmueble con id " + id));
    }

    @Transactional(readOnly = true)
    public Inmueble buscarPorClave(String clave) {
        return repository.findByClave(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el inmueble con clave " + clave));
    }

    @Transactional
    public Inmueble crear(Inmueble inmueble) {
        if (repository.existsByClave(inmueble.getClave())) {
            throw new RecursoDuplicadoException("Ya existe un inmueble con clave " + inmueble.getClave());
        }
        inmueble.setId(null); // el id lo genera PostgreSQL
        return repository.save(inmueble);
    }

    @Transactional
    public Inmueble actualizar(Long id, Inmueble datos) {
        Inmueble actual = buscarPorId(id);

        if (!actual.getClave().equals(datos.getClave()) && repository.existsByClave(datos.getClave())) {
            throw new RecursoDuplicadoException("Ya existe un inmueble con clave " + datos.getClave());
        }

        actual.setClave(datos.getClave());
        actual.setNombre(datos.getNombre());
        actual.setUbicacion(datos.getUbicacion());
        actual.setDescripcion(datos.getDescripcion());
        actual.setPrecio(datos.getPrecio());
        actual.setTerrenoM2(datos.getTerrenoM2());
        actual.setConstruccionM2(datos.getConstruccionM2());
        actual.setDistribucion(datos.getDistribucion());
        actual.setAmenidades(datos.getAmenidades());
        actual.setImagenUrl(datos.getImagenUrl());
        actual.setFichaTecnicaUrl(datos.getFichaTecnicaUrl());
        if (datos.getEstatus() != null) {
            actual.setEstatus(datos.getEstatus());
        }
        actual.setUsuarioId(datos.getUsuarioId());

        return repository.save(actual);
    }

    @Transactional
    public void eliminar(Long id) {
        Inmueble inmueble = buscarPorId(id);
        repository.delete(inmueble);
    }
}
