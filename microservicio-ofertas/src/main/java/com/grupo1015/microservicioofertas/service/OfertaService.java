package com.grupo1015.microservicioofertas.service;

import com.grupo1015.microservicioofertas.exception.OfertaNoEncontradaException;
import com.grupo1015.microservicioofertas.model.Oferta;
import com.grupo1015.microservicioofertas.model.TipoPublicacion;
import com.grupo1015.microservicioofertas.repository.OfertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfertaService {

    private final OfertaRepository repo;

    public List<Oferta> listar(TipoPublicacion tipo, String propiedad) {
        if (propiedad != null) return repo.findByPropiedadAndActivaTrue(propiedad);
        if (tipo != null) return repo.findByTipoAndActivaTrue(tipo);
        return repo.findByActivaTrue();
    }

    public Oferta obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new OfertaNoEncontradaException(id));
    }

    public Oferta crear(Oferta oferta) {
        validar(oferta);
        oferta.setId(null);
        oferta.setActiva(true);
        return repo.save(oferta);
    }

    public Oferta actualizar(Long id, Oferta datos) {
        validar(datos);
        Oferta oferta = obtener(id);
        oferta.setPropiedad(datos.getPropiedad());
        oferta.setTitulo(datos.getTitulo());
        oferta.setDescripcion(datos.getDescripcion());
        oferta.setTipo(datos.getTipo());
        oferta.setPrecioOriginal(datos.getPrecioOriginal());
        oferta.setPrecioFinal(datos.getPrecioFinal());
        oferta.setFechaInicio(datos.getFechaInicio());
        oferta.setFechaFin(datos.getFechaFin());
        return repo.save(oferta);
    }

    public Oferta desactivar(Long id) {
        Oferta oferta = obtener(id);
        oferta.setActiva(false);
        return repo.save(oferta);
    }

    public void borrar(Long id) {
        if (!repo.existsById(id)) {
            throw new OfertaNoEncontradaException(id);
        }
        repo.deleteById(id);
    }

    private void validar(Oferta o) {
        if (o.getPrecioFinal().compareTo(o.getPrecioOriginal()) > 0) {
            throw new IllegalArgumentException("El precio final no puede ser mayor que el original");
        }
        if (o.getFechaInicio() != null && o.getFechaFin() != null
                && o.getFechaFin().isBefore(o.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }
}