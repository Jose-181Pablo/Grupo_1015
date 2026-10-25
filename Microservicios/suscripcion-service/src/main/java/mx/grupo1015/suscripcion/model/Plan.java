package mx.grupo1015.suscripcion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Plan que se puede contratar (por ejemplo, PREMIUM mensual o anual).
 */
@Entity
@Table(name = "planes")
@Getter
@Setter
@NoArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "duracion_dias", nullable = false)
    private Integer duracionDias;

    @Column(nullable = false)
    private boolean activo = true;

    public Plan(
            String codigo,
            String nombre,
            String descripcion,
            BigDecimal precio,
            Integer duracionDias
    ) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.duracionDias = duracionDias;
    }
}
