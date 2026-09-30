package mx.grupo1015.chatia.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversaciones")
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", length = 100)
    private String usuarioId;

    @Column(name = "propiedad_id", nullable = false, length = 100)
    private String propiedadId;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;

    @OneToMany(
            mappedBy = "conversacion",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Mensaje> mensajes = new ArrayList<>();

    public Conversacion() {
    }

    public Conversacion(String usuarioId, String propiedadId, String titulo) {
        this.usuarioId = usuarioId;
        this.propiedadId = propiedadId;
        this.titulo = titulo;
    }

    @PrePersist
    public void antesDeGuardar() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        ultimaActualizacion = ahora;
    }

    @PreUpdate
    public void antesDeActualizar() {
        ultimaActualizacion = LocalDateTime.now();
    }

    public void agregarMensaje(Mensaje mensaje) {
        mensajes.add(mensaje);
        mensaje.setConversacion(this);
    }

    public void eliminarMensaje(Mensaje mensaje) {
        mensajes.remove(mensaje);
        mensaje.setConversacion(null);
    }

    public void marcarActualizada() {
        ultimaActualizacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getPropiedadId() {
        return propiedadId;
    }

    public void setPropiedadId(String propiedadId) {
        this.propiedadId = propiedadId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public List<Mensaje> getMensajes() {
        return mensajes;
    }


}