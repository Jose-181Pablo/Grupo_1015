package mx.grupo1015.suscripcion;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mx.grupo1015.suscripcion.client.AutenticacionClient;
import mx.grupo1015.suscripcion.dto.CuentaAutenticada;
import mx.grupo1015.suscripcion.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Levanta un servidor HTTP falso que responde como el microservicio de autenticación.
 */
class AutenticacionClientTests {

    private HttpServer servidor;
    private AutenticacionClient cliente;

    private final AtomicInteger logins = new AtomicInteger();
    private final List<String> cambiosDePlan = new CopyOnWriteArrayList<>();

    // Cuántas veces el PUT del plan contesta 401 antes de aceptar (simula un token vencido)
    private final AtomicInteger rechazosPendientes = new AtomicInteger();

    @BeforeEach
    void iniciarAutenticacionFalsa() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        servidor.createContext("/api/auth/validar", intercambio -> {
            String token = intercambio.getRequestHeaders().getFirst("Authorization");

            if ("Bearer token-maria".equals(token)) {
                responder(intercambio, 200, """
                        {
                          "cuentaId": 2,
                          "correo": "maria@correo.com",
                          "usuarioId": 2,
                          "rol": "USUARIO",
                          "plan": "ESTANDAR",
                          "accesoPremium": false
                        }
                        """);
            } else {
                responder(intercambio, 401, "{\"error\":\"Token invalido o vencido\"}");
            }
        });

        servidor.createContext("/api/auth/login", intercambio -> {
            int numero = logins.incrementAndGet();
            responder(intercambio, 200, """
                    { "token": "admin-%d", "tipo": "Bearer", "expiraEnMinutos": 60 }
                    """.formatted(numero));
        });

        servidor.createContext("/api/auth/admin/cuentas/", intercambio -> {
            if (rechazosPendientes.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                responder(intercambio, 401, "{\"error\":\"Token vencido\"}");
                return;
            }

            String cuerpo = new String(
                    intercambio.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            cambiosDePlan.add(
                    intercambio.getRequestMethod() + " "
                            + intercambio.getRequestURI().getPath() + " "
                            + intercambio.getRequestHeaders().getFirst("Authorization") + " "
                            + cuerpo.replace(" ", "")
            );
            responder(intercambio, 200, "{\"id\":2,\"plan\":\"PREMIUM\"}");
        });

        servidor.start();
        cliente = crearCliente("http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void detenerAutenticacionFalsa() {
        servidor.stop(0);
    }

    @Test
    void validaElToken() {
        Optional<CuentaAutenticada> cuenta = cliente.validar("Bearer token-maria");

        assertThat(cuenta).isPresent();
        assertThat(cuenta.get().cuentaId()).isEqualTo(2L);
        assertThat(cuenta.get().correo()).isEqualTo("maria@correo.com");
        assertThat(cuenta.get().accesoPremium()).isFalse();
    }

    @Test
    void regresaVacioSinTokenOConTokenInvalido() {
        assertThat(cliente.validar(null)).isEmpty();
        assertThat(cliente.validar("Bearer falso")).isEmpty();
    }

    @Test
    void avisaSiAutenticacionNoResponde() {
        // Puerto 1: no hay nada escuchando, la conexión se rechaza
        AutenticacionClient sinServicio = crearCliente("http://localhost:1");

        assertThatThrownBy(() -> sinServicio.validar("Bearer token-maria"))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void cambiaElPlanConLaCuentaDeServicio() {
        cliente.cambiarPlan(2L, "PREMIUM");
        cliente.cambiarPlan(2L, "ESTANDAR");

        // Inicia sesión una sola vez y reutiliza el token
        assertThat(logins.get()).isEqualTo(1);
        assertThat(cambiosDePlan).containsExactly(
                "PUT /api/auth/admin/cuentas/2/plan Bearer admin-1 {\"plan\":\"PREMIUM\"}",
                "PUT /api/auth/admin/cuentas/2/plan Bearer admin-1 {\"plan\":\"ESTANDAR\"}"
        );
    }

    @Test
    void vuelveAIniciarSesionSiElTokenVencio() {
        cliente.cambiarPlan(2L, "PREMIUM");
        rechazosPendientes.set(1);

        cliente.cambiarPlan(2L, "ESTANDAR");

        assertThat(logins.get()).isEqualTo(2);
        assertThat(cambiosDePlan).last().asString().contains("Bearer admin-2");
    }

    private AutenticacionClient crearCliente(String url) {
        return new AutenticacionClient(url, "servicio@grupo1015.com", "secreto");
    }

    private static void responder(HttpExchange intercambio, int estado, String json) throws IOException {
        byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);

        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(estado, cuerpo.length);

        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }
}
