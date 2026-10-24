package mx.grupo1015.chatia;

import com.sun.net.httpserver.HttpServer;
import mx.grupo1015.chatia.client.CatalogoClient;
import mx.grupo1015.chatia.dto.PropiedadCatalogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta un servidor HTTP falso que responde como inmobiliaria-service.
 */
class CatalogoClientTests {

    private static final String LAGO_NOGAL = """
            {
              "id": 13,
              "clave": "lago_nogal",
              "nombre": "Lago Nogal",
              "categoria": "TERRENO",
              "ubicacion": "Fracc. Lago Nogal, Zona Sur de Guadalajara",
              "precio": 5490000.00,
              "terrenoM2": 448.00,
              "construccionM2": null,
              "distribucion": "Terreno en esquina 16 x 28 m",
              "amenidades": "Alberca, Gym, Vapor, Sauna",
              "imagenUrl": "assets/images/LAGO_NOGAL/terreno_dia.jpg",
              "estatus": "DISPONIBLE",
              "usuarioId": null
            }
            """;

    private HttpServer servidor;
    private CatalogoClient cliente;

    @BeforeEach
    void iniciarCatalogoFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        servidor.createContext("/api/propiedades/clave/", intercambio -> {
            boolean existe = intercambio.getRequestURI().getPath().endsWith("/lago_nogal");
            byte[] cuerpo = (existe ? LAGO_NOGAL : "{\"error\":\"No existe\"}")
                    .getBytes(StandardCharsets.UTF_8);

            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(existe ? 200 : 404, cuerpo.length);

            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });

        servidor.start();
        cliente = new CatalogoClient("http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void detenerCatalogoFalso() {
        servidor.stop(0);
    }

    @Test
    void leeLaPropiedadDelCatalogo() {
        Optional<PropiedadCatalogo> propiedad = cliente.buscarPorClave("lago_nogal");

        assertThat(propiedad).isPresent();
        assertThat(propiedad.get().nombre()).isEqualTo("Lago Nogal");
        assertThat(propiedad.get().precio()).isEqualByComparingTo(new BigDecimal("5490000"));
        assertThat(propiedad.get().terrenoM2()).isEqualByComparingTo(new BigDecimal("448"));
    }

    @Test
    void regresaVacioSiLaPropiedadNoExiste() {
        assertThat(cliente.buscarPorClave("no_existe")).isEmpty();
    }

    @Test
    void regresaVacioSiElCatalogoNoResponde() {
        // Puerto 1: no hay nada escuchando, la conexión se rechaza
        CatalogoClient sinCatalogo = new CatalogoClient("http://localhost:1");

        assertThat(sinCatalogo.buscarPorClave("lago_nogal")).isEmpty();
    }
}
