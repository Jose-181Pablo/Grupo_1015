package mx.grupo1015.chatia.controller;

import jakarta.validation.Valid;
import mx.grupo1015.chatia.dto.ChatResponse;
import mx.grupo1015.chatia.dto.ConversacionResponse;
import mx.grupo1015.chatia.dto.CrearConversacionRequest;
import mx.grupo1015.chatia.dto.EnviarMensajeRequest;
import mx.grupo1015.chatia.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversaciones")
@CrossOrigin(
        originPatterns = {
                "http://localhost:*",
                "http://127.0.0.1:*"
        }
)
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ConversacionResponse> crearConversacion(
            @Valid @RequestBody CrearConversacionRequest request
    ) {
        ConversacionResponse conversacion =
                chatService.crearConversacion(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(conversacion);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversacionResponse> obtenerConversacion(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                chatService.obtenerConversacion(id)
        );
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<ChatResponse> enviarMensaje(
            @PathVariable Long id,
            @Valid @RequestBody EnviarMensajeRequest request
    ) {
        return ResponseEntity.ok(
                chatService.enviarMensaje(id, request)
        );
    }

    @GetMapping
    public ResponseEntity<List<ConversacionResponse>> obtenerPorUsuario(
            @RequestParam String usuarioId
    ) {
        return ResponseEntity.ok(
                chatService.obtenerPorUsuario(usuarioId)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarConversacion(
            @PathVariable Long id
    ) {
        chatService.eliminarConversacion(id);
        return ResponseEntity.noContent().build();
    }
}