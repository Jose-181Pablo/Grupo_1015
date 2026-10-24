package mx.grupo1015.chatia.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import mx.grupo1015.chatia.dto.PropiedadCatalogo;
import mx.grupo1015.chatia.exception.IaServiceException;
import mx.grupo1015.chatia.model.Conversacion;
import mx.grupo1015.chatia.model.Mensaje;
import mx.grupo1015.chatia.model.Remitente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class GeminiService {

    private final Client client;
    private final String modelo;

    public GeminiService(
            @Value("${gemini.model}") String modelo
    ) {
        this.client = new Client();
        this.modelo = modelo;
    }

    public String generarRespuesta(
            Conversacion conversacion,
            Optional<PropiedadCatalogo> propiedad,
            List<Mensaje> historial
    ) {
        String prompt = construirPrompt(conversacion, propiedad, historial);

        try {
            GenerateContentResponse respuesta =
                    client.models.generateContent(
                            modelo,
                            prompt,
                            null
                    );

            String texto = respuesta.text();

            if (texto == null || texto.isBlank()) {
                throw new IaServiceException(
                        "Gemini devolvió una respuesta vacía",
                        null
                );
            }

            return texto.trim();

        } catch (IaServiceException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IaServiceException(
                    "No fue posible obtener una respuesta de Gemini",
                    exception
            );
        }
    }

    private String construirPrompt(
            Conversacion conversacion,
            Optional<PropiedadCatalogo> propiedad,
            List<Mensaje> historial
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                Eres el asistente inmobiliario virtual de Grupo 10.15.

                Tu función es orientar al usuario sobre las propiedades
                del catálogo de manera clara, profesional y breve.

                Reglas:
                - Responde siempre en español.
                - No inventes precios, ubicaciones, características,
                  porcentajes de plusvalía ni datos técnicos.
                - Utiliza solamente la información proporcionada.
                - Si falta información, indícalo y recomienda consultar
                  a un asesor inmobiliario.
                - No afirmes que una inversión garantiza rendimientos.
                - No solicites datos personales sensibles.
                - Mantén la respuesta en un máximo de cuatro párrafos.

                """);

        prompt.append("Identificador de la propiedad: ")
                .append(conversacion.getPropiedadId())
                .append("\n");

        prompt.append("Título de la conversación: ")
                .append(conversacion.getTitulo())
                .append("\n\n");

        propiedad.ifPresentOrElse(
                datos -> agregarDatosPropiedad(prompt, datos),
                () -> prompt.append("""
                        No hay datos del catálogo disponibles para esta propiedad.
                        Indica al usuario que un asesor puede darle la información.

                        """)
        );

        prompt.append("Historial de la conversación:\n");

        for (Mensaje mensaje : historial) {
            if (mensaje.getRemitente() == Remitente.USUARIO) {
                prompt.append("Usuario: ");
            } else if (mensaje.getRemitente() == Remitente.ASISTENTE) {
                prompt.append("Asistente: ");
            } else {
                prompt.append("Sistema: ");
            }

            prompt.append(mensaje.getContenido())
                    .append("\n");
        }

        prompt.append("\nResponde al último mensaje del usuario.");

        return prompt.toString();
    }

    private void agregarDatosPropiedad(
            StringBuilder prompt,
            PropiedadCatalogo datos
    ) {
        prompt.append("Información de la propiedad según el catálogo:\n");

        agregarDato(prompt, "Nombre", datos.nombre());
        agregarDato(prompt, "Tipo", datos.categoria());
        agregarDato(prompt, "Ubicación", datos.ubicacion());
        agregarDato(prompt, "Descripción", datos.descripcion());
        agregarDato(prompt, "Precio (MXN)", formatear(datos.precio()));
        agregarDato(prompt, "Terreno (m²)", formatear(datos.terrenoM2()));
        agregarDato(prompt, "Construcción (m²)", formatear(datos.construccionM2()));
        agregarDato(prompt, "Distribución", datos.distribucion());
        agregarDato(prompt, "Amenidades", datos.amenidades());
        agregarDato(prompt, "Estatus", datos.estatus());

        prompt.append("\n");
    }

    private void agregarDato(StringBuilder prompt, String etiqueta, String valor) {
        if (valor != null && !valor.isBlank()) {
            prompt.append("- ")
                    .append(etiqueta)
                    .append(": ")
                    .append(valor)
                    .append("\n");
        }
    }

    private String formatear(BigDecimal numero) {
        return numero == null ? null : numero.stripTrailingZeros().toPlainString();
    }
}
