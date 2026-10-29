package mx.grupo1015.suscripcion.service;

import mx.grupo1015.suscripcion.client.AutenticacionClient;
import mx.grupo1015.suscripcion.dto.ContratarSuscripcionRequest;
import mx.grupo1015.suscripcion.dto.CuentaAutenticada;
import mx.grupo1015.suscripcion.dto.PlanResponse;
import mx.grupo1015.suscripcion.dto.SuscripcionResponse;
import mx.grupo1015.suscripcion.exception.NoAutenticadoException;
import mx.grupo1015.suscripcion.exception.RecursoNoEncontradoException;
import mx.grupo1015.suscripcion.exception.SuscripcionActivaException;
import mx.grupo1015.suscripcion.model.EstadoSuscripcion;
import mx.grupo1015.suscripcion.model.Plan;
import mx.grupo1015.suscripcion.model.Suscripcion;
import mx.grupo1015.suscripcion.repository.PlanRepository;
import mx.grupo1015.suscripcion.repository.SuscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SuscripcionService {

    private static final Logger log = LoggerFactory.getLogger(SuscripcionService.class);

    private static final String PLAN_PREMIUM = "PREMIUM";
    private static final String PLAN_ESTANDAR = "ESTANDAR";
    private static final String ROL_ADMIN = "ADMIN";

    private final PlanRepository planRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final AutenticacionClient autenticacionClient;

    public SuscripcionService(
            PlanRepository planRepository,
            SuscripcionRepository suscripcionRepository,
            AutenticacionClient autenticacionClient
    ) {
        this.planRepository = planRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.autenticacionClient = autenticacionClient;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> obtenerPlanes() {
        return planRepository.findByActivoTrueOrderByPrecioAsc()
                .stream()
                .map(this::convertirPlan)
                .toList();
    }

    @Transactional
    public SuscripcionResponse contratar(
            String authorization,
            ContratarSuscripcionRequest request
    ) {
        CuentaAutenticada cuenta = autenticar(authorization);

        // Un ADMIN ya tiene acceso PREMIUM por su rol; si contratara, al cancelar quedaría en ESTANDAR
        if (ROL_ADMIN.equals(cuenta.rol())) {
            throw new SuscripcionActivaException(
                    "Las cuentas de administrador ya tienen acceso PREMIUM y no necesitan contratar un plan"
            );
        }

        if (buscarActivaSincronizada(cuenta) != null) {
            throw new SuscripcionActivaException(
                    "Ya tienes una suscripción activa"
            );
        }

        Plan plan = planRepository.findByCodigoAndActivoTrue(request.planCodigo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el plan " + request.planCodigo()
                ));

        // Pago simulado: por ahora solo se genera una referencia
        String referenciaPago = "PAGO-" + UUID.randomUUID();

        // Primero se activa PREMIUM; si autenticación falla, no se guarda la suscripción
        autenticacionClient.cambiarPlan(cuenta.cuentaId(), PLAN_PREMIUM);

        Suscripcion suscripcion = new Suscripcion(
                cuenta.cuentaId(),
                cuenta.usuarioId(),
                cuenta.correo(),
                plan,
                referenciaPago,
                LocalDateTime.now()
        );

        return convertirSuscripcion(suscripcionRepository.save(suscripcion));
    }

    @Transactional
    public SuscripcionResponse obtenerActual(String authorization) {
        CuentaAutenticada cuenta = autenticar(authorization);
        Suscripcion activa = buscarActivaSincronizada(cuenta);

        if (activa == null) {
            throw new RecursoNoEncontradoException(
                    "No tienes una suscripción activa"
            );
        }

        return convertirSuscripcion(activa);
    }

    @Transactional
    public List<SuscripcionResponse> obtenerHistorial(String authorization) {
        CuentaAutenticada cuenta = autenticar(authorization);
        buscarActivaSincronizada(cuenta);

        return suscripcionRepository
                .findByCuentaIdOrderByFechaCreacionDesc(cuenta.cuentaId())
                .stream()
                .map(this::convertirSuscripcion)
                .toList();
    }

    @Transactional
    public SuscripcionResponse cancelar(String authorization) {
        CuentaAutenticada cuenta = autenticar(authorization);
        Suscripcion activa = buscarActivaSincronizada(cuenta);

        if (activa == null) {
            throw new RecursoNoEncontradoException(
                    "No tienes una suscripción activa"
            );
        }

        autenticacionClient.cambiarPlan(cuenta.cuentaId(), PLAN_ESTANDAR);

        activa.setEstado(EstadoSuscripcion.CANCELADA);
        activa.setFechaCancelacion(LocalDateTime.now());

        return convertirSuscripcion(suscripcionRepository.save(activa));
    }

    /**
     * Marca como VENCIDA cada suscripción cuya fecha de fin ya pasó y regresa la cuenta a ESTANDAR.
     * Si autenticación no responde, la suscripción se queda ACTIVA y se intenta en la siguiente revisión.
     */
    @Transactional
    public int vencerSuscripciones() {
        List<Suscripcion> vencidas = suscripcionRepository.findByEstadoAndFechaFinBefore(
                EstadoSuscripcion.ACTIVA,
                LocalDateTime.now()
        );

        int procesadas = 0;

        for (Suscripcion suscripcion : vencidas) {
            try {
                autenticacionClient.cambiarPlan(suscripcion.getCuentaId(), PLAN_ESTANDAR);
                suscripcion.setEstado(EstadoSuscripcion.VENCIDA);
                procesadas++;
            } catch (RuntimeException exception) {
                log.warn(
                        "No se pudo vencer la suscripción {}: {}",
                        suscripcion.getId(),
                        exception.getMessage()
                );
            }
        }

        return procesadas;
    }

    private CuentaAutenticada autenticar(String authorization) {
        return autenticacionClient.validar(authorization)
                .orElseThrow(() -> new NoAutenticadoException(
                        "Inicia sesión para administrar tu suscripción"
                ));
    }

    /**
     * Busca la suscripción activa y la compara con el plan real de la cuenta en autenticación.
     * Si un ADMIN regresó la cuenta a ESTANDAR por su lado, la suscripción se marca como CANCELADA
     * para que los dos microservicios digan lo mismo.
     */
    private Suscripcion buscarActivaSincronizada(CuentaAutenticada cuenta) {
        Suscripcion activa = buscarActiva(cuenta.cuentaId());

        if (activa != null && !PLAN_PREMIUM.equals(cuenta.plan())) {
            activa.setEstado(EstadoSuscripcion.CANCELADA);
            activa.setFechaCancelacion(LocalDateTime.now());
            suscripcionRepository.save(activa);

            log.info(
                    "La suscripción {} se canceló porque la cuenta {} ya no es PREMIUM en autenticación",
                    activa.getId(),
                    cuenta.cuentaId()
            );

            return null;
        }

        return activa;
    }

    private Suscripcion buscarActiva(Long cuentaId) {
        return suscripcionRepository
                .findFirstByCuentaIdAndEstadoOrderByFechaFinDesc(
                        cuentaId,
                        EstadoSuscripcion.ACTIVA
                )
                .filter(suscripcion -> suscripcion.getFechaFin().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    private PlanResponse convertirPlan(Plan plan) {
        return new PlanResponse(
                plan.getCodigo(),
                plan.getNombre(),
                plan.getDescripcion(),
                plan.getPrecio(),
                plan.getDuracionDias()
        );
    }

    private SuscripcionResponse convertirSuscripcion(Suscripcion suscripcion) {
        return new SuscripcionResponse(
                suscripcion.getId(),
                suscripcion.getCuentaId(),
                suscripcion.getUsuarioId(),
                suscripcion.getCorreo(),
                convertirPlan(suscripcion.getPlan()),
                suscripcion.getEstado(),
                suscripcion.getMonto(),
                suscripcion.getReferenciaPago(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaFin(),
                suscripcion.getFechaCancelacion()
        );
    }
}
