package com.grupo_1015.autenticacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Modelo: datos de acceso de un usuario (se guardan en la tabla "credenciales").
// Los datos personales (nombre, apellido) viven en el microservicio usuario;
// aqui solo se guarda el id con el que se relaciona (usuarioId).
@Entity
@Table(name = "credenciales")
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // la BD genera el id
    private Long id;

    @Column(nullable = false, unique = true) // no puede haber dos cuentas con el mismo correo
    private String correo;

    @Column(nullable = false) // se guarda cifrada, nunca en texto plano
    private String password;

    @Column(nullable = false)
    private Long usuarioId;

    public Credencial() {
    }

    public Credencial(String correo, String password, Long usuarioId) {
        this.correo = correo;
        this.password = password;
        this.usuarioId = usuarioId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}
