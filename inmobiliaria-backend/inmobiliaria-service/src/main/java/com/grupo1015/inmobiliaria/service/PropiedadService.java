package com.grupo1015.inmobiliaria.service;

import com.grupo1015.inmobiliaria.exception.RecursoDuplicadoException;
import com.grupo1015.inmobiliaria.exception.RecursoNoEncontradoException;
import com.grupo1015.inmobiliaria.model.Categoria;
import com.grupo1015.inmobiliaria.model.Propiedad;
import com.grupo1015.inmobiliaria.repository.PropiedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SERVICIO: aquí van las reglas de negocio.
 * El controlador nunca habla directo con el repositorio, siempre pasa por aquí.
 */
@Service
public class PropiedadService {

    private final PropiedadRepository repository;

    public PropiedadService(PropiedadRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Propiedad> listar(Categoria categoria, String ubicacion) {
        boolean hayUbicacion = ubicacion != null && !ubicacion.isBlank();
        if (categoria != null && hayUbicacion) {
            return repository.findByCategoriaAndUbicacionContainingIgnoreCase(categoria, ubicacion);
        }
        if (categoria != null) {
            return repository.findByCategoria(categoria);
        }
        if (hayUbicacion) {
            return repository.findByUbicacionContainingIgnoreCase(ubicacion);
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Propiedad buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la propiedad con id " + id));
    }

    @Transactional(readOnly = true)
    public Propiedad buscarPorClave(String clave) {
        return repository.findByClave(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la propiedad con clave " + clave));
    }

    @Transactional
    public Propiedad crear(Propiedad propiedad) {
        if (repository.existsByClave(propiedad.getClave())) {
            throw new RecursoDuplicadoException("Ya existe una propiedad con clave " + propiedad.getClave());
        }
        propiedad.setId(null); // el id lo genera PostgreSQL
        return repository.save(propiedad);
    }

    @Transactional
    public Propiedad actualizar(Long id, Propiedad datos) {
        Propiedad actual = buscarPorId(id);

        if (!actual.getClave().equals(datos.getClave()) && repository.existsByClave(datos.getClave())) {
            throw new RecursoDuplicadoException("Ya existe una propiedad con clave " + datos.getClave());
        }

        actual.setClave(datos.getClave());
        actual.setNombre(datos.getNombre());
        actual.setCategoria(datos.getCategoria());
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
        Propiedad propiedad = buscarPorId(id);
        repository.delete(propiedad);
    }
}
