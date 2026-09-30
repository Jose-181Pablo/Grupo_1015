package mx.grupo1015.chatia.service;

import mx.grupo1015.chatia.dto.ChatResponse;
import mx.grupo1015.chatia.dto.ConversacionResponse;
import mx.grupo1015.chatia.dto.CrearConversacionRequest;
import mx.grupo1015.chatia.dto.EnviarMensajeRequest;
import mx.grupo1015.chatia.dto.MensajeResponse;
import mx.grupo1015.chatia.exception.RecursoNoEncontradoException;
import mx.grupo1015.chatia.model.Conversacion;
import mx.grupo1015.chatia.model.Mensaje;
import mx.grupo1015.chatia.model.Remitente;
import mx.grupo1015.chatia.repository.ConversacionRepository;
import mx.grupo1015.chatia.repository.MensajeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;

    public ChatService(
            ConversacionRepository conversacionRepository,
            MensajeRepository mensajeRepository
    ) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
    }

    @Transactional
    public ConversacionResponse crearConversacion(
            CrearConversacionRequest request
    ) {
        String usuarioId = limpiarTextoOpcional(request.usuarioId());

        Conversacion conversacion = new Conversacion(
                usuarioId,
                request.propiedadId().trim(),
                request.titulo().trim()
        );

        Conversacion guardada =
                conversacionRepository.save(conversacion);

        return convertirConversacion(guardada, List.of());
    }

    @Transactional(readOnly = true)
    public ConversacionResponse obtenerConversacion(Long id) {
        Conversacion conversacion = buscarConversacion(id);

        List<Mensaje> mensajes =
                mensajeRepository
                        .findByConversacion_IdOrderByFechaCreacionAsc(id);

        return convertirConversacion(conversacion, mensajes);
    }

    @Transactional
    public ChatResponse enviarMensaje(
            Long conversacionId,
            EnviarMensajeRequest request
    ) {
        Conversacion conversacion =
                buscarConversacion(conversacionId);

        Mensaje mensajeUsuario = new Mensaje(
                Remitente.USUARIO,
                request.pregunta().trim()
        );

        mensajeUsuario.setConversacion(conversacion);

        Mensaje usuarioGuardado =
                mensajeRepository.save(mensajeUsuario);

        String contenidoRespuesta = generarRespuestaSimulada(
                conversacion,
                request.pregunta()
        );

        Mensaje mensajeAsistente = new Mensaje(
                Remitente.ASISTENTE,
                contenidoRespuesta
        );

        mensajeAsistente.setConversacion(conversacion);

        Mensaje asistenteGuardado =
                mensajeRepository.save(mensajeAsistente);

        conversacion.marcarActualizada();
        conversacionRepository.save(conversacion);

        return new ChatResponse(
                conversacion.getId(),
                convertirMensaje(usuarioGuardado),
                convertirMensaje(asistenteGuardado)
        );
    }

    @Transactional(readOnly = true)
    public List<ConversacionResponse> obtenerPorUsuario(
            String usuarioId
    ) {
        return conversacionRepository
                .findByUsuarioIdOrderByUltimaActualizacionDesc(
                        usuarioId
                )
                .stream()
                .map(conversacion -> {
                    List<Mensaje> mensajes =
                            mensajeRepository
                                    .findByConversacion_IdOrderByFechaCreacionAsc(
                                            conversacion.getId()
                                    );

                    return convertirConversacion(
                            conversacion,
                            mensajes
                    );
                })
                .toList();
    }

    @Transactional
    public void eliminarConversacion(Long id) {
        Conversacion conversacion = buscarConversacion(id);
        conversacionRepository.delete(conversacion);
    }

    private Conversacion buscarConversacion(Long id) {
        return conversacionRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe la conversación con id " + id
                        )
                );
    }

    private String generarRespuestaSimulada(
            Conversacion conversacion,
            String pregunta
    ) {
        return "Respuesta simulada para la propiedad "
                + conversacion.getPropiedadId()
                + ": recibí tu pregunta \""
                + pregunta.trim()
                + "\". Posteriormente esta respuesta será generada por Gemini.";
    }

    private ConversacionResponse convertirConversacion(
            Conversacion conversacion,
            List<Mensaje> mensajes
    ) {
        List<MensajeResponse> mensajesResponse = mensajes.stream()
                .map(this::convertirMensaje)
                .toList();

        return new ConversacionResponse(
                conversacion.getId(),
                conversacion.getUsuarioId(),
                conversacion.getPropiedadId(),
                conversacion.getTitulo(),
                conversacion.getFechaCreacion(),
                conversacion.getUltimaActualizacion(),
                mensajesResponse
        );
    }

    private MensajeResponse convertirMensaje(Mensaje mensaje) {
        return new MensajeResponse(
                mensaje.getId(),
                mensaje.getRemitente(),
                mensaje.getContenido(),
                mensaje.getFechaCreacion()
        );
    }

    private String limpiarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}