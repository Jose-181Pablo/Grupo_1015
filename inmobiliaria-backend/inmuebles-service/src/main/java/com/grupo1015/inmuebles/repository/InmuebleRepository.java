package com.grupo1015.inmuebles.repository;

import com.grupo1015.inmuebles.model.Estatus;
import com.grupo1015.inmuebles.model.Inmueble;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORIO: Spring genera el SQL a partir del nombre de cada método.
 * JpaRepository ya trae findAll, findById, save, deleteById, etc.
 */
public interface InmuebleRepository extends JpaRepository<Inmueble, Long> {

    Optional<Inmueble> findByClave(String clave);

    boolean existsByClave(String clave);

    List<Inmueble> findByEstatus(Estatus estatus);

    List<Inmueble> findByUbicacionContainingIgnoreCase(String texto);

    List<Inmueble> findByEstatusAndUbicacionContainingIgnoreCase(Estatus estatus, String texto);
}
