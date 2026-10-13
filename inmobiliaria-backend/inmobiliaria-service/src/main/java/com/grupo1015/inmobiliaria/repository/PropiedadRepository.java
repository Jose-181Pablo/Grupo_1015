package com.grupo1015.inmobiliaria.repository;

import com.grupo1015.inmobiliaria.model.Categoria;
import com.grupo1015.inmobiliaria.model.Propiedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORIO: Spring genera el SQL a partir del nombre de cada método.
 * JpaRepository ya trae findAll, findById, save, deleteById, etc.
 */
public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {

    Optional<Propiedad> findByClave(String clave);

    boolean existsByClave(String clave);

    List<Propiedad> findByCategoria(Categoria categoria);

    List<Propiedad> findByUbicacionContainingIgnoreCase(String texto);

    List<Propiedad> findByCategoriaAndUbicacionContainingIgnoreCase(Categoria categoria, String texto);
}
