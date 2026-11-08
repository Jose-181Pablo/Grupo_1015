package com.grupo1015.microservicioofertas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String propiedad;          // ej. "chapala", "casa-fuerte-baluarte"

    @NotBlank
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoPublicacion tipo;

    @NotNull @Positive
    @Column(precision = 14, scale = 2)
    private BigDecimal precioOriginal;

    @NotNull @Positive
    @Column(precision = 14, scale = 2)
    private BigDecimal precioFinal;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private boolean activa = true;
}