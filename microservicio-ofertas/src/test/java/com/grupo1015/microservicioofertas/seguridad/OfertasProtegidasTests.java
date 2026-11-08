package com.grupo1015.microservicioofertas.seguridad;

import com.grupo1015.microservicioofertas.client.AutenticacionClient;
import com.grupo1015.microservicioofertas.client.CuentaValidada;
import com.grupo1015.microservicioofertas.controller.OfertaController;
import com.grupo1015.microservicioofertas.exception.NoAutorizadoException;
import com.grupo1015.microservicioofertas.exception.ServicioNoDisponibleException;
import com.grupo1015.microservicioofertas.model.Oferta;
import com.grupo1015.microservicioofertas.service.OfertaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OfertaController.class)
class OfertasProtegidasTests {

    private static final String JSON = """
            {"propiedad":"casa65","titulo":"Casa 65 en oferta","tipo":"OFERTA",
             "precioOriginal":5000000,"precioFinal":4500000}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OfertaService service;

    @MockitoBean
    private AutenticacionClient autenticacionClient;

    @Test
    void consultarNoPideSesion() throws Exception {
        when(service.listar(any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/ofertas")).andExpect(status().isOk());

        verifyNoInteractions(autenticacionClient);
    }

    @Test
    void crearSinTokenResponde401() throws Exception {
        when(autenticacionClient.validar(null)).thenThrow(new NoAutorizadoException("Inicia sesión"));

        mvc.perform(post("/api/ofertas").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isUnauthorized());

        verify(service, never()).crear(any());
    }

    @Test
    void crearConUsuarioNormalResponde403() throws Exception {
        when(autenticacionClient.validar("Bearer abc"))
                .thenReturn(new CuentaValidada(2L, "user@correo.com", "USUARIO"));

        mvc.perform(post("/api/ofertas").header("Authorization", "Bearer abc")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isForbidden());

        verify(service, never()).crear(any());
    }

    @Test
    void crearConAdminResponde201() throws Exception {
        when(autenticacionClient.validar("Bearer admin"))
                .thenReturn(new CuentaValidada(1L, "admin@correo.com", "ADMIN"));
        when(service.crear(any(Oferta.class))).thenAnswer(i -> i.getArgument(0));

        mvc.perform(post("/api/ofertas").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void desactivarYBorrarTambienEstanProtegidos() throws Exception {
        when(autenticacionClient.validar(null)).thenThrow(new NoAutorizadoException("Inicia sesión"));

        mvc.perform(patch("/api/ofertas/1/desactivar")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/ofertas/1")).andExpect(status().isUnauthorized());

        verify(service, never()).desactivar(eq(1L));
        verify(service, never()).borrar(eq(1L));
    }

    @Test
    void autenticacionApagadaResponde503() throws Exception {
        when(autenticacionClient.validar("Bearer admin"))
                .thenThrow(new ServicioNoDisponibleException("No disponible"));

        mvc.perform(delete("/api/ofertas/1").header("Authorization", "Bearer admin"))
                .andExpect(status().isServiceUnavailable());
    }
}
