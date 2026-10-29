package mx.grupo1015.suscripcion.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import mx.grupo1015.suscripcion.dto.CuentaAutenticada;
import mx.grupo1015.suscripcion.exception.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Habla con el microservicio de autenticación (rama liz).
 * - validar: identifica a la persona con el token que manda la página.
 * - cambiarPlan: pone la cuenta en PREMIUM o ESTANDAR usando una cuenta de servicio con rol ADMIN.
 * La dirección de autenticación se pide a Eureka; si Eureka no la conoce o no responde,
 * se usa la URL fija de la configuración.
 */
@Component
public class AutenticacionClient {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionClient.class);

    private static final String SERVICIO_AUTENTICACION = "autenticacion";

    private final RestClient restClient;
    private final String urlAutenticacion;
    private final Optional<DiscoveryClient> discoveryClient;
    private final String correoServicio;
    private final String passwordServicio;

    // Token de la cuenta de servicio; se pide con login y se renueva cuando vence (401)
    private String tokenServicio;

    @Autowired
    public AutenticacionClient(
            @Value("${autenticacion.url}") String urlAutenticacion,
            @Value("${autenticacion.correo}") String correoServicio,
            @Value("${autenticacion.password}") String passwordServicio,
            Optional<DiscoveryClient> discoveryClient
    ) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(fabrica)
                .build();
        this.urlAutenticacion = urlAutenticacion;
        this.correoServicio = correoServicio;
        this.passwordServicio = passwordServicio;
        this.discoveryClient = discoveryClient;
    }

    public AutenticacionClient(
            String urlAutenticacion,
            String correoServicio,
            String passwordServicio
    ) {
        this(urlAutenticacion, correoServicio, passwordServicio, Optional.empty());
    }

    /**
     * Regresa la cuenta dueña del token, o vacío si no hay token o no es válido.
     */
    public Optional<CuentaAutenticada> validar(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return Optional.empty();
        }

        try {
            CuentaAutenticada cuenta = llamar(base -> restClient.get()
                    .uri(base + "/api/auth/validar")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(CuentaAutenticada.class));

            return Optional.ofNullable(cuenta);

        } catch (HttpClientErrorException.Unauthorized exception) {
            return Optional.empty();

        } catch (RestClientException exception) {
            log.warn("No fue posible validar el token: {}", exception.getMessage());
            throw new ServicioNoDisponibleException(
                    "El microservicio de autenticación no está disponible",
                    exception
            );
        }
    }

    /**
     * Cambia el plan de la cuenta ("PREMIUM" o "ESTANDAR").
     */
    public synchronized void cambiarPlan(Long cuentaId, String plan) {
        try {
            enviarCambioPlan(cuentaId, plan, obtenerTokenServicio());

        } catch (HttpClientErrorException.Unauthorized exception) {
            // El token de servicio venció: se inicia sesión otra vez y se reintenta una vez
            tokenServicio = null;
            intentarCambioPlan(cuentaId, plan);

        } catch (RestClientException exception) {
            throw errorAlCambiarPlan(exception);
        }
    }

    private void intentarCambioPlan(Long cuentaId, String plan) {
        try {
            enviarCambioPlan(cuentaId, plan, obtenerTokenServicio());
        } catch (RestClientException exception) {
            throw errorAlCambiarPlan(exception);
        }
    }

    private void enviarCambioPlan(Long cuentaId, String plan, String token) {
        llamar(base -> restClient.put()
                .uri(base + "/api/auth/admin/cuentas/{cuentaId}/plan", cuentaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("plan", plan))
                .retrieve()
                .toBodilessEntity());
    }

    private String obtenerTokenServicio() {
        if (tokenServicio != null) {
            return tokenServicio;
        }

        if (correoServicio == null || correoServicio.isBlank()
                || passwordServicio == null || passwordServicio.isBlank()) {
            throw new ServicioNoDisponibleException(
                    "Falta configurar la cuenta de servicio (AUTENTICACION_CORREO y AUTENTICACION_PASSWORD)"
            );
        }

        try {
            TokenRespuesta respuesta = llamar(base -> restClient.post()
                    .uri(base + "/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("correo", correoServicio, "password", passwordServicio))
                    .retrieve()
                    .body(TokenRespuesta.class));

            if (respuesta == null || respuesta.token() == null) {
                throw new ServicioNoDisponibleException(
                        "El microservicio de autenticación no regresó un token"
                );
            }

            tokenServicio = respuesta.token();
            return tokenServicio;

        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ServicioNoDisponibleException(
                    "La cuenta de servicio no pudo iniciar sesión en autenticación; revisa el correo y la contraseña",
                    exception
            );
        } catch (RestClientException exception) {
            log.warn("No fue posible iniciar sesión en autenticación: {}", exception.getMessage());
            throw new ServicioNoDisponibleException(
                    "El microservicio de autenticación no está disponible",
                    exception
            );
        }
    }

    /**
     * Hace la llamada con cada dirección posible (primero las de Eureka y al final la URL fija)
     * hasta que una responda. Solo cambia de dirección cuando no hubo conexión.
     */
    private <T> T llamar(Function<String, T> llamada) {
        ResourceAccessException sinConexion = null;

        for (String base : direcciones()) {
            try {
                return llamada.apply(base);
            } catch (ResourceAccessException exception) {
                log.warn("No fue posible conectar con autenticación en {}: {}", base, exception.getMessage());
                sinConexion = exception;
            }
        }

        throw sinConexion;
    }

    private Set<String> direcciones() {
        Set<String> direcciones = new LinkedHashSet<>();

        discoveryClient.ifPresent(cliente -> {
            try {
                cliente.getInstances(SERVICIO_AUTENTICACION)
                        .forEach(instancia -> direcciones.add(instancia.getUri().toString()));
            } catch (RuntimeException exception) {
                log.warn("No fue posible consultar Eureka: {}", exception.getMessage());
            }
        });

        direcciones.add(urlAutenticacion);
        return direcciones;
    }

    private ServicioNoDisponibleException errorAlCambiarPlan(RestClientException exception) {
        if (exception instanceof HttpClientErrorException.Forbidden) {
            return new ServicioNoDisponibleException(
                    "La cuenta de servicio no tiene rol ADMIN en autenticación",
                    exception
            );
        }

        log.warn("No fue posible cambiar el plan: {}", exception.getMessage());
        return new ServicioNoDisponibleException(
                "No fue posible actualizar el plan en el microservicio de autenticación",
                exception
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenRespuesta(String token, String tipo, long expiraEnMinutos) {
    }
}
