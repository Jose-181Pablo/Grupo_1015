package com.grupo_1015.autenticacion.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Cliente: se comunica por HTTP con el microservicio usuario.
// Es la unica clase de autenticacion que sabe como llamar a usuario.
@Component
public class UsuarioClient {

    private final RestClient restClient;

    // La URL viene de application.properties (servicios.usuario.url)
    public UsuarioClient(@Value("${servicios.usuario.url}") String usuarioUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(usuarioUrl)
                .build();
    }

    // Llama a POST /api/usuarios del microservicio usuario y devuelve el id creado
    public Long crearUsuario(String nombre, String apellido, String correo) {
        UsuarioResponse creado = restClient.post()
                .uri("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UsuarioRequest(nombre, apellido, correo))
                .retrieve()
                .body(UsuarioResponse.class);
        return creado.id();
    }

    // Lo que se envia a usuario (mismos campos que su clase Usuario)
    public record UsuarioRequest(String nombre, String apellido, String correo) {
    }

    // Lo que responde usuario
    public record UsuarioResponse(Long id, String nombre, String apellido, String correo) {
    }
}
