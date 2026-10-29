package mx.grupo1015.suscripcion;

import mx.grupo1015.suscripcion.client.AutenticacionClient;
import mx.grupo1015.suscripcion.dto.CuentaAutenticada;
import mx.grupo1015.suscripcion.exception.ServicioNoDisponibleException;
import mx.grupo1015.suscripcion.model.EstadoSuscripcion;
import mx.grupo1015.suscripcion.model.Plan;
import mx.grupo1015.suscripcion.model.Suscripcion;
import mx.grupo1015.suscripcion.repository.PlanRepository;
import mx.grupo1015.suscripcion.repository.SuscripcionRepository;
import mx.grupo1015.suscripcion.service.SuscripcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SuscripcionControllerTests {

    private static final String TOKEN_MARIA = "Bearer token-maria";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuscripcionService suscripcionService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @MockitoBean
    private AutenticacionClient autenticacionClient;

    @Test
    void muestraLosPlanes() throws Exception {
        mockMvc.perform(get("/api/v1/planes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("PREMIUM_MENSUAL"));
    }

    @Test
    void contrataConsultaYCancelaLaSuscripcion() throws Exception {
        when(autenticacionClient.validar(TOKEN_MARIA))
                .thenReturn(Optional.of(cuenta(2L, "maria@correo.com")));

        mockMvc.perform(post("/api/v1/suscripciones")
                        .header("Authorization", TOKEN_MARIA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "PREMIUM_MENSUAL" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuentaId").value(2))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.plan.duracionDias").value(30))
                .andExpect(jsonPath("$.monto").value(149.0))
                .andExpect(jsonPath("$.referenciaPago").exists());

        verify(autenticacionClient).cambiarPlan(2L, "PREMIUM");

        // Desde aquí autenticación ya responde que la cuenta es PREMIUM
        when(autenticacionClient.validar(TOKEN_MARIA))
                .thenReturn(Optional.of(cuenta(2L, "maria@correo.com", "PREMIUM")));

        mockMvc.perform(get("/api/v1/suscripciones/actual")
                        .header("Authorization", TOKEN_MARIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.codigo").value("PREMIUM_MENSUAL"));

        // No se puede contratar otra mientras la primera siga activa
        mockMvc.perform(post("/api/v1/suscripciones")
                        .header("Authorization", TOKEN_MARIA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "PREMIUM_ANUAL" }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/suscripciones/actual/cancelar")
                        .header("Authorization", TOKEN_MARIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.fechaCancelacion").exists());

        verify(autenticacionClient).cambiarPlan(2L, "ESTANDAR");

        mockMvc.perform(get("/api/v1/suscripciones/actual")
                        .header("Authorization", TOKEN_MARIA))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/suscripciones")
                        .header("Authorization", TOKEN_MARIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cancelaLaSuscripcionSiUnAdminQuitoPremiumEnAutenticacion() throws Exception {
        String token = "Bearer token-sofia";

        when(autenticacionClient.validar(token))
                .thenReturn(Optional.of(cuenta(5L, "sofia@correo.com")));

        mockMvc.perform(post("/api/v1/suscripciones")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "PREMIUM_ANUAL" }
                                """))
                .andExpect(status().isCreated());

        // El ADMIN regresó la cuenta a ESTANDAR directamente en autenticación
        mockMvc.perform(get("/api/v1/suscripciones/actual")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/suscripciones")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("CANCELADA"))
                .andExpect(jsonPath("$[0].fechaCancelacion").exists());

        // Ya puede volver a contratar
        mockMvc.perform(post("/api/v1/suscripciones")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "PREMIUM_MENSUAL" }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void noDejaContratarAUnAdmin() throws Exception {
        when(autenticacionClient.validar("Bearer token-admin"))
                .thenReturn(Optional.of(new CuentaAutenticada(
                        1L, "admin@grupo1015.com", null, "ADMIN", "PREMIUM", true
                )));

        mockMvc.perform(post("/api/v1/suscripciones")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "PREMIUM_MENSUAL" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").exists());

        verify(autenticacionClient, never()).cambiarPlan(any(), any());
    }

    @Test
    void pideIniciarSesionSiNoHayToken() throws Exception {
        when(autenticacionClient.validar(null)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/suscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "PREMIUM_MENSUAL" }
                                """))
                .andExpect(status().isUnauthorized());

        verify(autenticacionClient, never()).cambiarPlan(any(), any());
    }

    @Test
    void rechazaPlanesInvalidos() throws Exception {
        when(autenticacionClient.validar("Bearer token-luis"))
                .thenReturn(Optional.of(cuenta(3L, "luis@correo.com")));

        mockMvc.perform(post("/api/v1/suscripciones")
                        .header("Authorization", "Bearer token-luis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "NO_EXISTE" }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/suscripciones")
                        .header("Authorization", "Bearer token-luis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "planCodigo": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.planCodigo").exists());
    }

    @Test
    void responde503SiAutenticacionNoEstaDisponible() throws Exception {
        when(autenticacionClient.validar("Bearer token-ana"))
                .thenThrow(new ServicioNoDisponibleException(
                        "El microservicio de autenticación no está disponible"
                ));

        mockMvc.perform(get("/api/v1/suscripciones/actual")
                        .header("Authorization", "Bearer token-ana"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void venceLasSuscripcionesQueYaTerminaron() {
        Plan mensual = planRepository.findByCodigoAndActivoTrue("PREMIUM_MENSUAL").orElseThrow();
        Suscripcion vieja = suscripcionRepository.save(new Suscripcion(
                4L, 4L, "pedro@correo.com", mensual,
                "PAGO-prueba", LocalDateTime.now().minusDays(40)
        ));

        assertThat(suscripcionService.vencerSuscripciones()).isEqualTo(1);

        verify(autenticacionClient).cambiarPlan(4L, "ESTANDAR");
        assertThat(suscripcionRepository.findById(vieja.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSuscripcion.VENCIDA);
    }

    private CuentaAutenticada cuenta(Long cuentaId, String correo) {
        return cuenta(cuentaId, correo, "ESTANDAR");
    }

    private CuentaAutenticada cuenta(Long cuentaId, String correo, String plan) {
        return new CuentaAutenticada(cuentaId, correo, cuentaId, "USUARIO", plan, "PREMIUM".equals(plan));
    }
}
