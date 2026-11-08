package com.grupo_1015.autenticacion.repository;

import com.grupo_1015.autenticacion.model.Credencial;
import com.grupo_1015.autenticacion.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio: acceso a la tabla "credenciales".
// Spring Data JPA genera automaticamente findAll, findById, save, etc.
public interface CredencialRepository extends JpaRepository<Credencial, Long> {

    // Spring genera la consulta a partir del nombre del metodo:
    // SELECT COUNT(*) > 0 FROM credenciales WHERE correo = ?
    boolean existsByCorreo(String correo);

    // SELECT * FROM credenciales WHERE correo = ?
    Optional<Credencial> findByCorreo(String correo);

    // SELECT COUNT(*) FROM credenciales WHERE rol = ?
    long countByRol(Rol rol);
}
