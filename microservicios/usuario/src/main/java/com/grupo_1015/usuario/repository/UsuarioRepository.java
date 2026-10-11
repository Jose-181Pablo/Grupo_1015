package com.grupo_1015.usuario.repository;

import com.grupo_1015.usuario.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Repositorio: guarda los usuarios.
// Por ahora en memoria (se pierden al reiniciar). En la etapa de base de datos
// se reemplazará por una interfaz de Spring Data JPA con los mismos métodos.
@Repository
public class UsuarioRepository {

    private final Map<Long, Usuario> usuarios = new HashMap<>();
    private Long siguienteId = 1L;

    public List<Usuario> findAll() {
        return new ArrayList<>(usuarios.values());
    }

    public Optional<Usuario> findById(Long id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {
            usuario.setId(siguienteId);
            siguienteId++;
        }
        usuarios.put(usuario.getId(), usuario);
        return usuario;
    }

    public boolean existsById(Long id) {
        return usuarios.containsKey(id);
    }

    public void deleteById(Long id) {
        usuarios.remove(id);
    }
}
