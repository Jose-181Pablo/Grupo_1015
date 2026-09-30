package mx.grupo1015.chatia.exception;

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
        Map<String, Object> respuesta = crearRespuestaBase(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(respuesta);
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
        Map<String, Object> respuesta = crearRespuestaBase(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(respuesta);
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