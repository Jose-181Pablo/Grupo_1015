(() => {
    "use strict";

    const API_BASE_URL = "http://localhost:8080/api/v1";

    // Clave acordada con el login (microservicio de autenticación de Liz):
    // ahí se guarda el token después de iniciar sesión.
    const STORAGE_TOKEN = "grupo1015_token";

    const formatoMoneda = new Intl.NumberFormat("es-MX", {
        style: "currency",
        currency: "MXN"
    });

    const nombresEstado = {
        ACTIVA: "Activa",
        CANCELADA: "Cancelada",
        VENCIDA: "Vencida"
    };

    let planes = [];
    let suscripcionActual = null;

    function obtenerToken() {
        return localStorage.getItem(STORAGE_TOKEN);
    }

    function guardarToken(token) {
        localStorage.setItem(STORAGE_TOKEN, token);
    }

    function quitarToken() {
        localStorage.removeItem(STORAGE_TOKEN);
    }

    async function realizarPeticion(url, opciones = {}) {
        const token = obtenerToken();
        const encabezados = { ...(opciones.headers || {}) };

        if (token) {
            encabezados.Authorization = `Bearer ${token}`;
        }

        const response = await fetch(url, { ...opciones, headers: encabezados });

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

    function formatearFecha(texto) {
        if (!texto) {
            return "";
        }

        return new Date(texto).toLocaleDateString("es-MX", {
            day: "numeric",
            month: "long",
            year: "numeric"
        });
    }

    function mostrarMensaje(texto, tipo = "info") {
        const mensaje = document.getElementById("suscripcion-mensaje");
        mensaje.textContent = texto;
        mensaje.className = `suscripcion-mensaje ${tipo}`;
        mensaje.hidden = !texto;
    }

    function mensajeDeError(error) {
        if (error.status === undefined) {
            return "No hay conexión con el servidor. Revisa que el API Gateway (8080) y suscripcion-service estén encendidos.";
        }

        return error.message;
    }

    function crearElemento(etiqueta, clase, texto) {
        const elemento = document.createElement(etiqueta);

        if (clase) {
            elemento.className = clase;
        }

        if (texto !== undefined) {
            elemento.textContent = texto;
        }

        return elemento;
    }

    // ===== Sesión =====

    function pintarSesion() {
        const conToken = Boolean(obtenerToken());

        document.getElementById("sesion-sin-token").hidden = conToken;
        document.getElementById("sesion-con-token").hidden = !conToken;
        document.getElementById("mi-suscripcion").hidden = !conToken;
    }

    function cerrarSesionInvalida() {
        quitarToken();
        suscripcionActual = null;
        pintarSesion();
        pintarPlanes();
        mostrarMensaje("Tu sesión ya no es válida. Inicia sesión de nuevo.", "error");
    }

    // ===== Planes =====

    async function cargarPlanes() {
        const contenedor = document.getElementById("planes");

        try {
            planes = await realizarPeticion(`${API_BASE_URL}/planes`);
            pintarPlanes();
        } catch (error) {
            contenedor.replaceChildren(
                crearElemento("p", "suscripcion-vacio", mensajeDeError(error))
            );
        }
    }

    function pintarPlanes() {
        const contenedor = document.getElementById("planes");
        const conToken = Boolean(obtenerToken());

        const tarjetas = planes.map(plan => {
            const tarjeta = crearElemento("div", "plan-card");
            const esActual = suscripcionActual?.plan?.codigo === plan.codigo;

            tarjeta.append(
                crearElemento("span", "plan-duracion", `${plan.duracionDias} días`),
                crearElemento("h3", "", plan.nombre),
                crearElemento("p", "plan-precio", formatoMoneda.format(plan.precio)),
                crearElemento("p", "plan-descripcion", plan.descripcion || "")
            );

            const boton = crearElemento("button", "btn btn-primary");
            boton.type = "button";

            if (esActual) {
                boton.textContent = "Tu plan actual";
                boton.disabled = true;
            } else if (suscripcionActual) {
                boton.textContent = "Ya tienes un plan";
                boton.disabled = true;
            } else if (!conToken) {
                boton.textContent = "Inicia sesión para contratar";
                boton.disabled = true;
            } else {
                boton.textContent = "Contratar";
                boton.addEventListener("click", () => contratar(plan));
            }

            tarjeta.append(boton);
            return tarjeta;
        });

        contenedor.replaceChildren(...tarjetas);
    }

    async function contratar(plan) {
        const confirmado = window.confirm(
            `¿Contratar ${plan.nombre} por ${formatoMoneda.format(plan.precio)}?\n\nEl pago es simulado: no se hace ningún cargo real.`
        );

        if (!confirmado) {
            return;
        }

        try {
            await realizarPeticion(`${API_BASE_URL}/suscripciones`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ planCodigo: plan.codigo })
            });

            mostrarMensaje(`¡Listo! Ya tienes ${plan.nombre}.`, "exito");
            await cargarMiSuscripcion();
        } catch (error) {
            if (error.status === 401) {
                cerrarSesionInvalida();
                return;
            }

            mostrarMensaje(mensajeDeError(error), "error");
        }
    }

    // ===== Mi suscripción =====

    async function cargarMiSuscripcion() {
        if (!obtenerToken()) {
            return;
        }

        try {
            suscripcionActual = await realizarPeticion(`${API_BASE_URL}/suscripciones/actual`);
        } catch (error) {
            if (error.status === 401) {
                cerrarSesionInvalida();
                return;
            }

            suscripcionActual = null;

            if (error.status !== 404) {
                mostrarMensaje(mensajeDeError(error), "error");
            }
        }

        pintarMiSuscripcion();
        pintarPlanes();
        await cargarHistorial();
    }

    function pintarMiSuscripcion() {
        const contenedor = document.getElementById("suscripcion-actual");

        if (!suscripcionActual) {
            contenedor.replaceChildren(
                crearElemento("p", "suscripcion-vacio", "No tienes una suscripción activa. Elige un plan arriba.")
            );
            return;
        }

        const tarjeta = crearElemento("div", "actual-card");
        const datos = crearElemento("dl", "actual-datos");

        [
            ["Plan", suscripcionActual.plan.nombre],
            ["Estado", nombresEstado[suscripcionActual.estado] || suscripcionActual.estado],
            ["Pagado", formatoMoneda.format(suscripcionActual.monto)],
            ["Desde", formatearFecha(suscripcionActual.fechaInicio)],
            ["Vence", formatearFecha(suscripcionActual.fechaFin)],
            ["Referencia de pago", suscripcionActual.referenciaPago]
        ].forEach(([etiqueta, valor]) => {
            datos.append(crearElemento("dt", "", etiqueta), crearElemento("dd", "", valor));
        });

        const botonCancelar = crearElemento("button", "btn", "Cancelar suscripción");
        botonCancelar.type = "button";
        botonCancelar.addEventListener("click", cancelar);

        tarjeta.append(datos, botonCancelar);
        contenedor.replaceChildren(tarjeta);
    }

    async function cancelar() {
        const confirmado = window.confirm(
            "¿Seguro que quieres cancelar? Perderás el acceso PREMIUM desde este momento."
        );

        if (!confirmado) {
            return;
        }

        try {
            await realizarPeticion(`${API_BASE_URL}/suscripciones/actual/cancelar`, {
                method: "POST"
            });

            mostrarMensaje("Tu suscripción se canceló. Tu cuenta regresó al plan estándar.", "info");
            await cargarMiSuscripcion();
        } catch (error) {
            if (error.status === 401) {
                cerrarSesionInvalida();
                return;
            }

            mostrarMensaje(mensajeDeError(error), "error");
        }
    }

    async function cargarHistorial() {
        const cuerpo = document.getElementById("historial-cuerpo");
        const seccion = document.getElementById("historial");

        try {
            const historial = await realizarPeticion(`${API_BASE_URL}/suscripciones`);

            seccion.hidden = historial.length === 0;

            cuerpo.replaceChildren(...historial.map(suscripcion => {
                const fila = document.createElement("tr");

                fila.append(
                    crearElemento("td", "", suscripcion.plan.nombre),
                    crearElemento("td", "", nombresEstado[suscripcion.estado] || suscripcion.estado),
                    crearElemento("td", "", formatoMoneda.format(suscripcion.monto)),
                    crearElemento("td", "", formatearFecha(suscripcion.fechaInicio)),
                    crearElemento("td", "", formatearFecha(suscripcion.fechaCancelacion || suscripcion.fechaFin))
                );

                return fila;
            }));
        } catch {
            seccion.hidden = true;
        }
    }

    // ===== Modo de prueba: pegar el token a mano =====
    // Temporal mientras existe la página de inicio de sesión.
    // Se obtiene con POST http://localhost:8080/api/auth/login en Postman.

    function configurarModoPrueba() {
        const formulario = document.getElementById("form-token");
        const campo = document.getElementById("campo-token");

        formulario.addEventListener("submit", async evento => {
            evento.preventDefault();

            const token = campo.value.trim().replace(/^Bearer\s+/i, "");

            if (!token) {
                return;
            }

            guardarToken(token);
            campo.value = "";
            mostrarMensaje("", "info");
            pintarSesion();
            await cargarMiSuscripcion();
        });

        document.getElementById("boton-quitar-token").addEventListener("click", () => {
            quitarToken();
            suscripcionActual = null;
            mostrarMensaje("", "info");
            pintarSesion();
            pintarPlanes();
        });
    }

    document.addEventListener("DOMContentLoaded", async () => {
        configurarModoPrueba();
        pintarSesion();
        await cargarPlanes();
        await cargarMiSuscripcion();
    });
})();
