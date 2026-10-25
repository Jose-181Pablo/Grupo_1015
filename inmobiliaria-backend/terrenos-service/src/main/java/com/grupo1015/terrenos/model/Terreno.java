package com.grupo1015.terrenos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MODELO: un terreno del catálogo.
 * Los campos salen de la lista "project-meta" de cada terreno en inmobiliaria.html
 * (Ubicación, Inversión, Superficie, Medidas, Vocación, Amenidades, Ficha técnica...).
 */
@Entity
@Table(name = "terrenos")
public class Terreno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador corto que ya usa la página, p. ej. "lago_nogal". */
    @NotBlank(message = "La clave es obligatoria")
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String ubicacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Precio en MXN. Puede ir vacío si la página no lo muestra. */
    @Positive(message = "El precio debe ser mayor a 0")
    @Column(precision = 14, scale = 2)
    private BigDecimal precio;

    @Positive(message = "La superficie debe ser mayor a 0")
    @Column(name = "superficie_m2", precision = 10, scale = 2)
    private BigDecimal superficieM2;

    /** Frente x fondo, p. ej. "16 x 28 m (esquina)". */
    @Size(max = 100)
    @Column(length = 100)
    private String medidas;

    /** Para qué se puede usar, p. ej. "Habitacional" o "Desarrollo Residencial Vertical". */
    @Size(max = 150)
    @Column(name = "uso_suelo", length = 150)
    private String usoSuelo;

    @Size(max = 500)
    @Column(length = 500)
    private String amenidades;

    @Size(max = 255)
    @Column(name = "imagen_url")
    private String imagenUrl;

    @Size(max = 255)
    @Column(name = "ficha_tecnica_url")
    private String fichaTecnicaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estatus estatus = Estatus.DISPONIBLE;

    /**
     * Id del usuario (asesor) que lo dio de alta.
     * Hoy es opcional; cuando exista el microservicio de autenticación con JWT,
     * se llenará automáticamente con el id que venga dentro del token.
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
        if (estatus == null) {
            estatus = Estatus.DISPONIBLE;
        }
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    // ----- Getters y setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public BigDecimal getSuperficieM2() { return superficieM2; }
    public void setSuperficieM2(BigDecimal superficieM2) { this.superficieM2 = superficieM2; }

    public String getMedidas() { return medidas; }
    public void setMedidas(String medidas) { this.medidas = medidas; }

    public String getUsoSuelo() { return usoSuelo; }
    public void setUsoSuelo(String usoSuelo) { this.usoSuelo = usoSuelo; }

    public String getAmenidades() { return amenidades; }
    public void setAmenidades(String amenidades) { this.amenidades = amenidades; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getFichaTecnicaUrl() { return fichaTecnicaUrl; }
    public void setFichaTecnicaUrl(String fichaTecnicaUrl) { this.fichaTecnicaUrl = fichaTecnicaUrl; }

    public Estatus getEstatus() { return estatus; }
    public void setEstatus(Estatus estatus) { this.estatus = estatus; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
