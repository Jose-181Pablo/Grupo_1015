package mx.grupo1015.chatia;

import com.sun.net.httpserver.HttpServer;
import mx.grupo1015.chatia.client.OfertasClient;
import mx.grupo1015.chatia.dto.OfertaVigente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta un servidor HTTP falso que responde como microservicio-ofertas.
 */
class OfertasClientTests {

    private HttpServer servidor;
    private OfertasClient cliente;

    @BeforeEach
    void iniciarOfertasFalso() throws IOException {
        String vigente = LocalDate.now().plusDays(30).toString();
        String vencida = LocalDate.now().minusDays(1).toString();

        String ciudadela = """
                [
                  {
                    "id": 2,
                    "propiedad": "ciudadela",
                    "titulo": "La Ciudadela 45 en oferta",
                    "descripcion": "Precio especial por tiempo limitado.",
                    "tipo": "OFERTA",
                    "precioOriginal": 6300000.00,
                    "precioFinal": 5600000.00,
                    "fechaInicio": "2026-11-01",
                    "fechaFin": "%s",
                    "activa": true
                  },
                  {
                    "id": 7,
                    "propiedad": "ciudadela",
                    "titulo": "Oferta vieja",
                    "tipo": "OFERTA",
                    "precioOriginal": 6300000.00,
                    "precioFinal": 6000000.00,
                    "fechaFin": "%s",
                    "activa": true
                  }
                ]
                """.formatted(vigente, vencida);

        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/api/ofertas", intercambio -> {
            String consulta = intercambio.getRequestURI().getQuery();
            String cuerpo = "propiedad=ciudadela".equals(consulta) ? ciudadela : "[]";
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();

        cliente = new OfertasClient("http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void detenerOfertasFalso() {
        servidor.stop(0);
    }

    @Test
    void regresaSoloLasOfertasVigentes() {
        List<OfertaVigente> ofertas = cliente.buscarVigentes("ciudadela");

        assertThat(ofertas).hasSize(1);
        assertThat(ofertas.get(0).tipo()).isEqualTo("OFERTA");
        assertThat(ofertas.get(0).precioOriginal()).isEqualByComparingTo(new BigDecimal("6300000"));
        assertThat(ofertas.get(0).precioFinal()).isEqualByComparingTo(new BigDecimal("5600000"));
    }

    @Test
    void regresaVacioSiLaPropiedadNoTieneOfertas() {
        assertThat(cliente.buscarVigentes("hacienda")).isEmpty();
    }

    @Test
    void regresaVacioSiOfertasEstaApagado() {
        // Puerto 1: no hay nada escuchando, la conexión se rechaza
        OfertasClient sinOfertas = new OfertasClient("http://localhost:1");

        assertThat(sinOfertas.buscarVigentes("ciudadela")).isEmpty();
    }
}
