package mx.grupo1015.chatia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mx.grupo1015.chatia.client.CatalogoClient;
import mx.grupo1015.chatia.dto.PropiedadCatalogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta un servidor HTTP falso que responde como inmuebles-service y terrenos-service.
 */
class CatalogoClientTests {

    private static final String CASA_65 = """
            {
              "id": 1,
              "clave": "casa65",
              "nombre": "Casa 65",
              "ubicacion": "Valle Imperial",
              "precio": null,
              "terrenoM2": null,
              "construccionM2": 280.00,
              "distribucion": "3 recámaras",
              "estatus": "DISPONIBLE"
            }
            """;

    private static final String LAGO_NOGAL = """
            {
              "id": 2,
              "clave": "lago_nogal",
              "nombre": "Lago Nogal",
              "ubicacion": "Fracc. Lago Nogal, Zona Sur de Guadalajara",
              "precio": 5490000.00,
              "superficieM2": 448.00,
              "medidas": "16 x 28 m",
              "usoSuelo": "Habitacional",
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

        servidor.createContext("/api/inmuebles/clave/", intercambio ->
                responder(intercambio, "/casa65", CASA_65));
        servidor.createContext("/api/terrenos/clave/", intercambio ->
                responder(intercambio, "/lago_nogal", LAGO_NOGAL));

        servidor.start();
        String url = "http://localhost:" + servidor.getAddress().getPort();
        cliente = new CatalogoClient(url, url);
    }

    @AfterEach
    void detenerCatalogoFalso() {
        servidor.stop(0);
    }

    @Test
    void leeLasCasasDeInmuebles() {
        Optional<PropiedadCatalogo> propiedad = cliente.buscarPorClave("casa65");

        assertThat(propiedad).isPresent();
        assertThat(propiedad.get().nombre()).isEqualTo("Casa 65");
        assertThat(propiedad.get().categoria()).isEqualTo("CASA");
        assertThat(propiedad.get().construccionM2()).isEqualByComparingTo(new BigDecimal("280"));
    }

    @Test
    void leeLosTerrenosDeTerrenos() {
        Optional<PropiedadCatalogo> propiedad = cliente.buscarPorClave("lago_nogal");

        assertThat(propiedad).isPresent();
        assertThat(propiedad.get().nombre()).isEqualTo("Lago Nogal");
        assertThat(propiedad.get().categoria()).isEqualTo("TERRENO");
        assertThat(propiedad.get().precio()).isEqualByComparingTo(new BigDecimal("5490000"));
        assertThat(propiedad.get().terrenoM2()).isEqualByComparingTo(new BigDecimal("448"));
        assertThat(propiedad.get().usoSuelo()).isEqualTo("Habitacional");
    }

    @Test
    void regresaVacioSiLaPropiedadNoExiste() {
        assertThat(cliente.buscarPorClave("no_existe")).isEmpty();
    }

    @Test
    void buscaEnTerrenosAunqueInmueblesEsteApagado() {
        // Puerto 1: no hay nada escuchando, la conexión se rechaza
        CatalogoClient sinInmuebles = new CatalogoClient(
                "http://localhost:1",
                "http://localhost:" + servidor.getAddress().getPort()
        );

        assertThat(sinInmuebles.buscarPorClave("lago_nogal")).isPresent();
    }

    @Test
    void regresaVacioSiElCatalogoNoResponde() {
        CatalogoClient sinCatalogo = new CatalogoClient("http://localhost:1", "http://localhost:1");

        assertThat(sinCatalogo.buscarPorClave("lago_nogal")).isEmpty();
    }

    @Test
    void usaLaDireccionQueDaEureka() {
        int puerto = servidor.getAddress().getPort();
        DiscoveryClient eureka = eurekaFalso(puerto);

        // Las URLs fijas no responden: solo funciona si usa la dirección de Eureka
        CatalogoClient conEureka = new CatalogoClient(
                "http://localhost:1", "http://localhost:1", Optional.of(eureka)
        );

        assertThat(conEureka.buscarPorClave("casa65")).isPresent();
        assertThat(conEureka.buscarPorClave("lago_nogal")).isPresent();
    }

    @Test
    void usaLaUrlFijaSiEurekaDaUnaDireccionQueNoResponde() {
        String url = "http://localhost:" + servidor.getAddress().getPort();
        CatalogoClient conEurekaViejo = new CatalogoClient(url, url, Optional.of(eurekaFalso(1)));

        assertThat(conEurekaViejo.buscarPorClave("lago_nogal")).isPresent();
    }

    private static DiscoveryClient eurekaFalso(int puerto) {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "Eureka falso";
            }

            @Override
            public List<ServiceInstance> getInstances(String servicio) {
                return List.of(new DefaultServiceInstance(
                        servicio + "-1", servicio, "localhost", puerto, false
                ));
            }

            @Override
            public List<String> getServices() {
                return List.of("inmuebles-service", "terrenos-service");
            }
        };
    }

    private static void responder(HttpExchange intercambio, String clave, String json) throws IOException {
        boolean existe = intercambio.getRequestURI().getPath().endsWith(clave);
        byte[] cuerpo = (existe ? json : "{\"error\":\"No existe\"}")
                .getBytes(StandardCharsets.UTF_8);

        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(existe ? 200 : 404, cuerpo.length);

        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }
}
