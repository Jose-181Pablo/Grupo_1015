package com.grupo1015.microservicioofertas.config;

import com.grupo1015.microservicioofertas.model.Oferta;
import com.grupo1015.microservicioofertas.model.TipoPublicacion;
import com.grupo1015.microservicioofertas.repository.OfertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga 3 casas de la inmobiliaria como oferta/remate la primera vez que
 * arranca el servicio (solo si la tabla esta vacia).
 * El campo "propiedad" coincide con el id de la casa en inmobiliaria.html.
 */
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final OfertaRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        repo.save(nueva("hacienda", "Hacienda Agave en oferta",
                "Casa con alberca y cochera para 4 a 6 autos, a precio especial por tiempo limitado.",
                TipoPublicacion.OFERTA, "7800000", "7300000", LocalDate.of(2026, 12, 31)));

        repo.save(nueva("ciudadela", "La Ciudadela 45 en oferta",
                "Casa en La Ciudadela con precio especial por tiempo limitado.",
                TipoPublicacion.OFERTA, "6300000", "5900000", LocalDate.of(2026, 12, 31)));

        repo.save(nueva("baluarte", "Remate Valuarte 67",
                "Casa fuerte en Valuarte a precio de remate. Oportunidad unica de inversion.",
                TipoPublicacion.REMATE, "5200000", "4400000", LocalDate.of(2026, 12, 15)));
    }

    private Oferta nueva(String propiedad, String titulo, String descripcion,
                         TipoPublicacion tipo, String original, String finalPrecio, LocalDate fin) {
        Oferta o = new Oferta();
        o.setPropiedad(propiedad);
        o.setTitulo(titulo);
        o.setDescripcion(descripcion);
        o.setTipo(tipo);
        o.setPrecioOriginal(new BigDecimal(original));
        o.setPrecioFinal(new BigDecimal(finalPrecio));
        o.setFechaInicio(LocalDate.now());
        o.setFechaFin(fin);
        return o;
    }
}
