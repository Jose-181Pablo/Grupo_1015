package mx.grupo1015.chatia;

import com.jayway.jsonpath.JsonPath;
import mx.grupo1015.chatia.dto.PropiedadCatalogo;
import mx.grupo1015.chatia.client.CatalogoClient;
import mx.grupo1015.chatia.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ChatControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiService geminiService;

    @MockitoBean
    private CatalogoClient catalogoClient;

    @Test
    void creaConversacionEnviaMensajeYLaElimina() throws Exception {
        PropiedadCatalogo lagoNogal = new PropiedadCatalogo(
                "lago_nogal", "Lago Nogal", "TERRENO",
                "Fracc. Lago Nogal, Zona Sur de Guadalajara", null,
                new BigDecimal("5490000"), new BigDecimal("448"), null,
                null, "16 x 28 m", "Habitacional", null, "DISPONIBLE"
        );

        when(catalogoClient.buscarPorClave("lago_nogal"))
                .thenReturn(Optional.of(lagoNogal));
        when(geminiService.generarRespuesta(any(), any(), anyList()))
                .thenReturn("Respuesta de prueba");

        String creada = mockMvc.perform(post("/api/v1/conversaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": "usuario-1",
                                  "propiedadId": "lago_nogal",
                                  "titulo": "Consulta sobre Lago Nogal"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propiedadId").value("lago_nogal"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer id = JsonPath.read(creada, "$.id");

        mockMvc.perform(post("/api/v1/conversaciones/{id}/mensajes", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "pregunta": "¿Qué información tienes?" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajeUsuario.remitente").value("USUARIO"))
                .andExpect(jsonPath("$.respuestaAsistente.contenido")
                        .value("Respuesta de prueba"));

        // Los datos del catálogo deben llegar a Gemini
        verify(geminiService).generarRespuesta(any(), eq(Optional.of(lagoNogal)), anyList());

        mockMvc.perform(get("/api/v1/conversaciones/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajes.length()").value(2));

        mockMvc.perform(delete("/api/v1/conversaciones/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/conversaciones/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaPreguntaVacia() throws Exception {
        mockMvc.perform(post("/api/v1/conversaciones/1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "pregunta": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pregunta").exists());
    }
}
