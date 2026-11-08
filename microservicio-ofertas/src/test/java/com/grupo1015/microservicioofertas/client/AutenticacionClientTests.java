package com.grupo1015.microservicioofertas.client;

import com.grupo1015.microservicioofertas.exception.NoAutorizadoException;
import com.grupo1015.microservicioofertas.exception.ServicioNoDisponibleException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacionClientTests {

    private HttpServer servidor;

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private String levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/api/auth/validar", intercambio -> {
            String auth = intercambio.getRequestHeaders().getFirst("Authorization");
            boolean valido = "Bearer bueno".equals(auth);
            byte[] cuerpo = (valido
                    ? "{\"cuentaId\":1,\"correo\":\"admin@correo.com\",\"usuarioId\":1,\"rol\":\"ADMIN\",\"plan\":\"PREMIUM\",\"accesoPremium\":true}"
                    : "{\"error\":\"Token invalido\"}").getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(valido ? 200 : 401, cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    @Test
    void tokenValidoRegresaLaCuenta() throws IOException {
        AutenticacionClient cliente = new AutenticacionClient(levantar(), Optional.empty());

        CuentaValidada cuenta = cliente.validar("Bearer bueno");

        assertEquals("ADMIN", cuenta.rol());
        assertTrue(cuenta.esAdmin());
    }

    @Test
    void tokenInvalidoLanza401() throws IOException {
        AutenticacionClient cliente = new AutenticacionClient(levantar(), Optional.empty());

        assertThrows(NoAutorizadoException.class, () -> cliente.validar("Bearer malo"));
        assertThrows(NoAutorizadoException.class, () -> cliente.validar(null));
    }

    @Test
    void autenticacionApagadaLanza503() {
        AutenticacionClient cliente = new AutenticacionClient("http://localhost:1", Optional.empty());

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.validar("Bearer bueno"));
    }
}
