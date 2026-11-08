package com.grupo_1015.usuario.repository;

import com.grupo_1015.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio: acceso a la base de datos.
// Spring Data JPA genera automaticamente findAll, findById, save,
// existsById, deleteById, etc. No hace falta escribir SQL.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
