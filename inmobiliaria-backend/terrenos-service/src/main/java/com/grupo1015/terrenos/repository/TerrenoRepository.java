package com.grupo1015.terrenos.repository;

import com.grupo1015.terrenos.model.Estatus;
import com.grupo1015.terrenos.model.Terreno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORIO: Spring genera el SQL a partir del nombre de cada método.
 * JpaRepository ya trae findAll, findById, save, deleteById, etc.
 */
public interface TerrenoRepository extends JpaRepository<Terreno, Long> {

    Optional<Terreno> findByClave(String clave);

    boolean existsByClave(String clave);

    List<Terreno> findByEstatus(Estatus estatus);

    List<Terreno> findByUbicacionContainingIgnoreCase(String texto);

    List<Terreno> findByEstatusAndUbicacionContainingIgnoreCase(Estatus estatus, String texto);
}
