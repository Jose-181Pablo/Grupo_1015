package mx.grupo1015.suscripcion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Suscripción contratada por una cuenta del microservicio de autenticación.
 */
@Entity
@Table(name = "suscripciones")
@Getter
@NoArgsConstructor
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 150)
    private String correo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "referencia_pago", nullable = false, length = 60)
    private String referenciaPago;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Setter
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;

    public Suscripcion(
            Long cuentaId,
            Long usuarioId,
            String correo,
            Plan plan,
            String referenciaPago,
            LocalDateTime fechaInicio
    ) {
        this.cuentaId = cuentaId;
        this.usuarioId = usuarioId;
        this.correo = correo;
        this.plan = plan;
        this.estado = EstadoSuscripcion.ACTIVA;
        this.monto = plan.getPrecio();
        this.referenciaPago = referenciaPago;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaInicio.plusDays(plan.getDuracionDias());
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
}
