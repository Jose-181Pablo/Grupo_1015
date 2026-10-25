package mx.grupo1015.suscripcion.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request
    ) {
        return responder(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoAutenticado(
            NoAutenticadoException exception,
            HttpServletRequest request
    ) {
        return responder(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(SuscripcionActivaException.class)
    public ResponseEntity<Map<String, Object>> manejarSuscripcionActiva(
            SuscripcionActivaException exception,
            HttpServletRequest request
    ) {
        return responder(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarServicioNoDisponible(
            ServicioNoDisponibleException exception,
            HttpServletRequest request
    ) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> campos = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        campos.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> respuesta = crearRespuestaBase(
                HttpStatus.BAD_REQUEST,
                "Los datos enviados no son válidos",
                request.getRequestURI()
        );

        respuesta.put("campos", campos);

        return ResponseEntity
                .badRequest()
                .body(respuesta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorGeneral(
            Exception exception,
            HttpServletRequest request
    ) {
        return responder(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor",
                request
        );
    }

    private ResponseEntity<Map<String, Object>> responder(
            HttpStatus estado,
            String mensaje,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(estado)
                .body(crearRespuestaBase(estado, mensaje, request.getRequestURI()));
    }

    private Map<String, Object> crearRespuestaBase(
            HttpStatus estado,
            String mensaje,
            String ruta
    ) {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("fecha", LocalDateTime.now());
        respuesta.put("estado", estado.value());
        respuesta.put("error", estado.getReasonPhrase());
        respuesta.put("mensaje", mensaje);
        respuesta.put("ruta", ruta);

        return respuesta;
    }
}
