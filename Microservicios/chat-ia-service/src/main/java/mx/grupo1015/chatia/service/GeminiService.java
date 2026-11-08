package mx.grupo1015.chatia.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import mx.grupo1015.chatia.dto.OfertaVigente;
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
            List<OfertaVigente> ofertas,
            List<Mensaje> historial
    ) {
        String prompt = construirPrompt(conversacion, propiedad, ofertas, historial);

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
            List<OfertaVigente> ofertas,
            List<Mensaje> historial
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                Eres el asistente inmobiliario virtual de Grupo 10.15.

                Tu función es orientar al usuario sobre las propiedades
                del catálogo de manera clara, profesional y breve.

                Reglas:
                - Responde siempre en español.
                - No inventes precios, ubicaciones, características
                  ni datos técnicos.
                - Si preguntan por plusvalía o si el valor puede aumentar,
                  da una estimación orientativa: menciona el precio actual,
                  qué características y qué tipo de zona la favorecen
                  (ubicación, amenidades, servicios, crecimiento de la zona)
                  y un rango aproximado de plusvalía anual (por ejemplo
                  "entre 5% y 8% anual") según el mercado inmobiliario
                  de la Zona Metropolitana de Guadalajara. Aclara siempre
                  que es una estimación aproximada y no una garantía.
                - Fuera de esa estimación, utiliza solamente la información proporcionada.
                - Si falta información, indícalo y recomienda consultar
                  a un asesor inmobiliario.
                - No afirmes que una inversión garantiza rendimientos.
                - No solicites datos personales sensibles.
                - Si la propiedad tiene una oferta o un remate vigente,
                  el precio actual es el precio de la oferta o remate,
                  no el del catálogo. Menciona el precio original,
                  el precio con descuento y hasta cuándo es válido.
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

        agregarOfertas(prompt, ofertas);

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
        agregarDato(prompt, "Superficie del terreno (m²)", formatear(datos.terrenoM2()));
        agregarDato(prompt, "Construcción (m²)", formatear(datos.construccionM2()));
        agregarDato(prompt, "Distribución", datos.distribucion());
        agregarDato(prompt, "Medidas", datos.medidas());
        agregarDato(prompt, "Uso de suelo", datos.usoSuelo());
        agregarDato(prompt, "Amenidades", datos.amenidades());
        agregarDato(prompt, "Estatus", datos.estatus());

        prompt.append("\n");
    }

    private void agregarOfertas(
            StringBuilder prompt,
            List<OfertaVigente> ofertas
    ) {
        if (ofertas == null || ofertas.isEmpty()) {
            prompt.append("La propiedad no tiene ofertas ni remates vigentes.\n\n");
            return;
        }

        prompt.append("Ofertas y remates vigentes (su precio sustituye al del catálogo):\n");

        for (OfertaVigente oferta : ofertas) {
            prompt.append("- ")
                    .append("REMATE".equalsIgnoreCase(oferta.tipo()) ? "Remate" : "Oferta")
                    .append(": ")
                    .append(oferta.titulo())
                    .append("\n");

            agregarDato(prompt, "  Precio original (MXN)", formatear(oferta.precioOriginal()));
            agregarDato(prompt, "  Precio actual con descuento (MXN)", formatear(oferta.precioFinal()));
            agregarDato(prompt, "  Válido hasta", oferta.fechaFin() == null ? null : oferta.fechaFin().toString());
            agregarDato(prompt, "  Detalle", oferta.descripcion());
        }

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
