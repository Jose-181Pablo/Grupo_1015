package com.grupo1015.terrenos.service;

import com.grupo1015.terrenos.exception.RecursoDuplicadoException;
import com.grupo1015.terrenos.exception.RecursoNoEncontradoException;
import com.grupo1015.terrenos.model.Estatus;
import com.grupo1015.terrenos.model.Terreno;
import com.grupo1015.terrenos.repository.TerrenoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SERVICIO: aquí van las reglas de negocio.
 * El controlador nunca habla directo con el repositorio, siempre pasa por aquí.
 */
@Service
public class TerrenoService {

    private final TerrenoRepository repository;

    public TerrenoService(TerrenoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Terreno> listar(Estatus estatus, String ubicacion) {
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
    public Terreno buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el terreno con id " + id));
    }

    @Transactional(readOnly = true)
    public Terreno buscarPorClave(String clave) {
        return repository.findByClave(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el terreno con clave " + clave));
    }

    @Transactional
    public Terreno crear(Terreno terreno) {
        if (repository.existsByClave(terreno.getClave())) {
            throw new RecursoDuplicadoException("Ya existe un terreno con clave " + terreno.getClave());
        }
        terreno.setId(null); // el id lo genera PostgreSQL
        return repository.save(terreno);
    }

    @Transactional
    public Terreno actualizar(Long id, Terreno datos) {
        Terreno actual = buscarPorId(id);

        if (!actual.getClave().equals(datos.getClave()) && repository.existsByClave(datos.getClave())) {
            throw new RecursoDuplicadoException("Ya existe un terreno con clave " + datos.getClave());
        }

        actual.setClave(datos.getClave());
        actual.setNombre(datos.getNombre());
        actual.setUbicacion(datos.getUbicacion());
        actual.setDescripcion(datos.getDescripcion());
        actual.setPrecio(datos.getPrecio());
        actual.setSuperficieM2(datos.getSuperficieM2());
        actual.setMedidas(datos.getMedidas());
        actual.setUsoSuelo(datos.getUsoSuelo());
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
        Terreno terreno = buscarPorId(id);
        repository.delete(terreno);
    }
}
