(() => {
    "use strict";

    const API_BASE_URL = "http://localhost:8084/api/v1";
    const STORAGE_CONVERSACIONES = "grupo1015_chat_conversaciones";
    const STORAGE_USUARIO = "grupo1015_usuario_chat";

    const nombresPropiedades = {
        casa65: "Casa 65",
        almendrado: "Coto Almendrado",
        ciudadela: "La Ciudadela 45",
        baluarte: "Valuarte 67",
        hacienda: "Hacienda Agave",
        tlajomulco: "Hacienda Agave Tlajomulco",
        capri: "Jardín Real - Coto Capri",
        tajo: "San José del Tajo",
        sendero_monte_verde: "Sendero Monte Verde",
        chapala: "Casa en Chapala",
        bosques_santa_anita_2: "Casa en Bosques de Santa Anita",
        galerias_santa_anita: "Terreno Galerías Santa Anita",
        lago_nogal: "Terreno Lago Nogal",
        bosques_santa_anita: "Terreno Bosques de Santa Anita"
    };

    function obtenerUsuarioId() {
        let usuarioId = localStorage.getItem(STORAGE_USUARIO);

        if (!usuarioId) {
            const identificador = window.crypto?.randomUUID
                ? window.crypto.randomUUID()
                : Date.now().toString();

            usuarioId = `usuario-web-${identificador}`;
            localStorage.setItem(STORAGE_USUARIO, usuarioId);
        }

        return usuarioId;
    }

    function obtenerConversacionesGuardadas() {
        try {
            return JSON.parse(
                localStorage.getItem(STORAGE_CONVERSACIONES) || "{}"
            );
        } catch {
            return {};
        }
    }

    function guardarConversaciones(conversaciones) {
        localStorage.setItem(
            STORAGE_CONVERSACIONES,
            JSON.stringify(conversaciones)
        );
    }

    async function realizarPeticion(url, opciones) {
        const response = await fetch(url, opciones);

        let contenido = null;

        try {
            contenido = await response.json();
        } catch {
            contenido = null;
        }

        if (!response.ok) {
            const error = new Error(
                contenido?.mensaje ||
                `La petición terminó con estado ${response.status}`
            );

            error.status = response.status;
            throw error;
        }

        return contenido;
    }

    async function crearConversacion(projectId) {
        const nombrePropiedad =
            nombresPropiedades[projectId] || projectId;

        const conversacion = await realizarPeticion(
            `${API_BASE_URL}/conversaciones`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    usuarioId: obtenerUsuarioId(),
                    propiedadId: projectId,
                    titulo: `Consulta sobre ${nombrePropiedad}`
                })
            }
        );

        const conversaciones = obtenerConversacionesGuardadas();
        conversaciones[projectId] = conversacion.id;
        guardarConversaciones(conversaciones);

        return conversacion.id;
    }

    async function obtenerConversacionId(projectId) {
        const conversaciones = obtenerConversacionesGuardadas();

        if (conversaciones[projectId]) {
            return conversaciones[projectId];
        }

        return crearConversacion(projectId);
    }

    function olvidarConversacion(projectId) {
        const conversaciones = obtenerConversacionesGuardadas();
        delete conversaciones[projectId];
        guardarConversaciones(conversaciones);
    }

    async function enviarPregunta(projectId, pregunta, permitirReintento = true) {
        const conversacionId =
            await obtenerConversacionId(projectId);

        try {
            return await realizarPeticion(
                `${API_BASE_URL}/conversaciones/${conversacionId}/mensajes`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        pregunta: pregunta
                    })
                }
            );
        } catch (error) {
            /*
             * La conversación guardada en el navegador puede dejar de existir
             * si se elimina el volumen de Docker. En ese caso se crea otra.
             */
            if (error.status === 404 && permitirReintento) {
                olvidarConversacion(projectId);
                await crearConversacion(projectId);

                return enviarPregunta(projectId, pregunta, false);
            }

            throw error;
        }
    }

    function agregarMensaje(chat, contenido, tipo) {
        const mensaje = document.createElement("div");
        mensaje.classList.add("msg", tipo);
        mensaje.textContent = contenido;
        chat.appendChild(mensaje);
        chat.scrollTop = chat.scrollHeight;

        return mensaje;
    }

    window.useSuggestion = function (projectId, texto) {
        const input = document.getElementById(`input-${projectId}`);

        if (!input) {
            return;
        }

        input.value = texto;
        window.askAI(projectId);
    };

    window.askAI = async function (projectId) {
        const input = document.getElementById(`input-${projectId}`);
        const chat = document.getElementById(`ai-chat-${projectId}`);

        if (!input || !chat) {
            console.error(`No se encontró el chat de ${projectId}`);
            return;
        }

        const pregunta = input.value.trim();

        if (!pregunta) {
            return;
        }

        const boton = input
            .closest(".ai-input-group")
            ?.querySelector(".ai-btn");

        agregarMensaje(chat, pregunta, "msg-user");

        input.value = "";
        input.disabled = true;

        if (boton) {
            boton.disabled = true;
        }

        const mensajeEspera = agregarMensaje(
            chat,
            "Consultando al asistente de Grupo 10.15...",
            "msg-ai"
        );

        try {
            const respuesta = await enviarPregunta(
                projectId,
                pregunta
            );

            mensajeEspera.textContent =
                respuesta.respuestaAsistente.contenido;
        } catch (error) {
            console.error("Error al comunicarse con el chat:", error);

            if (error instanceof TypeError) {
                mensajeEspera.textContent =
                    "No fue posible conectar con el microservicio. " +
                    "Comprueba que Docker esté activo y que el servicio " +
                    "se encuentre disponible en el puerto 8084.";
            } else {
                mensajeEspera.textContent =
                    error.message ||
                    "Ocurrió un error al consultar al asistente.";
            }
        } finally {
            input.disabled = false;

            if (boton) {
                boton.disabled = false;
            }

            input.focus();
            chat.scrollTop = chat.scrollHeight;
        }
    };

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll(".ai-input").forEach(input => {
            input.addEventListener("keydown", event => {
                if (event.key !== "Enter") {
                    return;
                }

                event.preventDefault();

                const projectId =
                    input.id.replace("input-", "");

                window.askAI(projectId);
            });
        });
    });
})();