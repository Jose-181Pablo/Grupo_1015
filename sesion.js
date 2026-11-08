// sesion.js: inicio de sesión en una ventana flotante, desde cualquier página del sitio.
// - Los enlaces a login.html (botón "Iniciar sesión" del menú) abren la ventana en lugar de otra página.
// - Usa el microservicio autenticacion de Liz por el API Gateway:
//     POST /api/auth/login   -> {token, tipo, expiraEnMinutos}
//     GET  /api/auth/validar -> {cuentaId, correo, usuarioId, rol, plan, accesoPremium}
// - Guarda el token en localStorage "grupo1015_token" (la misma clave que usan
//   frontend/usuario y suscripcion.html).
// - Con sesión iniciada, el botón del menú cambia a "Cerrar sesión".
// - Permisos por rol:
//     INVITADO (sin sesión): no puede usar el chat con IA ni la página de contacto.
//     USUARIO: el chat con IA solo si tiene PREMIUM.
//     ADMIN: todo (siempre PREMIUM) y ve la pestaña "Panel de admin" junto al logo.
//   Estos bloqueos son de la página; autenticación ya protege sus rutas de admin.
// - Otras páginas pueden escuchar el evento "grupo1015:sesion" (detail = cuenta o null).
// Si no carga este archivo, los enlaces siguen llevando a login.html como antes.
(function () {
    "use strict";

    const API_URL = "http://localhost:8080/api";
    const CLAVE_TOKEN = "grupo1015_token";
    const PAGINA_REGISTRO = "frontend/usuario/index.html";

    // ---------- Token en localStorage ----------

    function leerToken() {
        try {
            return localStorage.getItem(CLAVE_TOKEN);
        } catch (e) {
            return null;
        }
    }

    function guardarToken(token) {
        try {
            localStorage.setItem(CLAVE_TOKEN, token);
        } catch (e) {
            // sin localStorage no se puede guardar la sesión
        }
    }

    function borrarToken() {
        try {
            localStorage.removeItem(CLAVE_TOKEN);
        } catch (e) {
            // nada que borrar
        }
    }

    // ---------- Peticiones a la API ----------

    // Devuelve el JSON; si falla lanza un Error con "status" (0 = sin conexión)
    async function peticion(ruta, opciones) {
        let respuesta;
        try {
            respuesta = await fetch(API_URL + ruta, opciones);
        } catch (e) {
            const error = new Error("Sin conexión");
            error.status = 0;
            throw error;
        }

        let datos = null;
        try {
            datos = await respuesta.json();
        } catch (e) {
            datos = null;
        }

        if (!respuesta.ok) {
            const error = new Error("Error " + respuesta.status);
            error.status = respuesta.status;
            throw error;
        }
        return datos;
    }

    function iniciarSesion(correo, password) {
        return peticion("/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ correo: correo, password: password })
        });
    }

    function validarSesion(token) {
        return peticion("/auth/validar", {
            headers: { "Authorization": "Bearer " + token }
        });
    }

    function textoDeError(error) {
        switch (error.status) {
            case 0:
                return "No se pudo conectar con el servidor. Revisa que los microservicios estén encendidos.";
            case 400:
                return "Faltan datos o no son válidos.";
            case 401:
                return "El correo o la contraseña no son correctos.";
            default:
                return "Ocurrió un error (código " + error.status + "). Intenta de nuevo.";
        }
    }

    // ---------- Ventana flotante ----------

    let fondo = null;
    let formulario = null;
    let mensaje = null;
    let boton = null;
    let ultimoFoco = null;
    let destinoTrasLogin = null; // página a la que se va después de iniciar sesión (o null = recargar)

    function crearVentana() {
        fondo = document.createElement("div");
        fondo.className = "sesion-fondo";
        fondo.hidden = true;
        fondo.innerHTML = `
            <div class="sesion-ventana" role="dialog" aria-modal="true" aria-labelledby="sesion-titulo">
                <button type="button" class="sesion-cerrar" aria-label="Cerrar">&times;</button>
                <span class="sesion-subtitulo">Acceso</span>
                <h2 id="sesion-titulo">Iniciar sesión</h2>
                <p class="sesion-texto">Escribe tu correo y tu contraseña para entrar a tu cuenta.</p>

                <form class="sesion-formulario" novalidate>
                    <label for="sesion-correo">Correo electrónico</label>
                    <input type="email" id="sesion-correo" name="correo" autocomplete="email"
                           placeholder="tucorreo@ejemplo.com" required>

                    <label for="sesion-password">Contraseña</label>
                    <input type="password" id="sesion-password" name="password"
                           autocomplete="current-password" placeholder="Tu contraseña" required>

                    <p class="sesion-mensaje" role="alert" hidden></p>

                    <button type="submit" class="sesion-boton">Iniciar sesión</button>
                </form>

                <p class="sesion-registro">
                    ¿No tienes cuenta? <a href="${PAGINA_REGISTRO}">Regístrate</a>
                </p>
            </div>`;

        document.body.appendChild(fondo);

        formulario = fondo.querySelector("form");
        mensaje = fondo.querySelector(".sesion-mensaje");
        boton = fondo.querySelector(".sesion-boton");

        fondo.querySelector(".sesion-cerrar").addEventListener("click", cerrarVentana);
        formulario.addEventListener("submit", enviar);
        document.addEventListener("keydown", function (evento) {
            if (evento.key === "Escape" && fondo && !fondo.hidden) {
                cerrarVentana();
            }
        });
    }

    function abrirVentana(destino) {
        destinoTrasLogin = typeof destino === "string" ? destino : null;
        if (!fondo) {
            crearVentana();
        }
        ultimoFoco = document.activeElement;
        ocultarMensaje();
        formulario.reset();
        boton.disabled = false;
        boton.textContent = "Iniciar sesión";

        fondo.hidden = false;
        document.body.classList.add("sesion-abierta");
        // Un cuadro después para que se vea la transición
        requestAnimationFrame(function () {
            fondo.classList.add("visible");
        });
        formulario.correo.focus();
    }

    function cerrarVentana() {
        if (!fondo) {
            return;
        }
        fondo.classList.remove("visible");
        fondo.hidden = true;
        document.body.classList.remove("sesion-abierta");
        if (ultimoFoco && typeof ultimoFoco.focus === "function") {
            ultimoFoco.focus();
        }
    }

    function mostrarMensaje(texto, tipo) {
        mensaje.textContent = texto;
        mensaje.className = "sesion-mensaje " + tipo;
        mensaje.hidden = false;
    }

    function ocultarMensaje() {
        mensaje.hidden = true;
    }

    async function enviar(evento) {
        evento.preventDefault();

        const correo = formulario.correo.value.trim();
        const password = formulario.password.value;

        if (!correo || !password) {
            mostrarMensaje("Escribe tu correo y tu contraseña.", "error");
            return;
        }
        if (!formulario.correo.checkValidity()) {
            mostrarMensaje("Escribe un correo electrónico válido.", "error");
            return;
        }

        ocultarMensaje();
        boton.disabled = true;
        boton.textContent = "Entrando...";

        try {
            const respuesta = await iniciarSesion(correo, password);
            await validarSesion(respuesta.token); // confirma que el token sirve
            guardarToken(respuesta.token);

            mostrarMensaje("Sesión iniciada.", "exito");
            // Se recarga la página para que todo (menú, suscripción, etc.) tome la sesión
            setTimeout(function () {
                if (destinoTrasLogin) {
                    window.location.href = destinoTrasLogin;
                } else {
                    window.location.reload();
                }
            }, 600);
        } catch (error) {
            borrarToken();
            mostrarMensaje(textoDeError(error), "error");
            boton.disabled = false;
            boton.textContent = "Iniciar sesión";
        }
    }

    // ---------- Botón del menú ----------

    function enlacesDeLogin() {
        return document.querySelectorAll('a[href="login.html"]');
    }

    function pintarSesionIniciada(cuenta) {
        document.querySelectorAll("nav a.nav-login").forEach(function (enlace) {
            enlace.textContent = "Cerrar sesión";
            enlace.dataset.sesion = "activa";
            enlace.title = cuenta && cuenta.correo
                ? "Sesión iniciada como " + cuenta.correo
                : "Sesión iniciada";
        });
    }

    function pintarInvitado() {
        document.querySelectorAll("nav a.nav-login").forEach(function (enlace) {
            enlace.textContent = "Iniciar sesión";
            delete enlace.dataset.sesion;
            enlace.removeAttribute("title");
        });
    }

    function cerrarSesion() {
        borrarToken();
        // El historial del asistente virtual se borra junto con la sesión.
        try {
            sessionStorage.removeItem("grupo1015_chat_conversaciones");
        } catch (e) { /* sin almacenamiento disponible */ }
        window.location.reload();
    }

    function alHacerClic(evento) {
        evento.preventDefault();
        if (this.dataset.sesion === "activa") {
            cerrarSesion();
        } else {
            abrirVentana();
        }
    }

    async function revisarSesion() {
        const token = leerToken();
        if (!token) {
            pintarInvitado();
            aplicarPermisos(null);
            return;
        }

        // Mientras responde el servidor se muestra como iniciada para que no parpadee
        pintarSesionIniciada(null);

        try {
            const cuenta = await validarSesion(token);
            pintarSesionIniciada(cuenta);
            aplicarPermisos(cuenta);
        } catch (error) {
            if (error.status === 401 || error.status === 403) {
                // Token vencido o inválido
                borrarToken();
                pintarInvitado();
                aplicarPermisos(null);
            } else {
                // Sin conexión: no se sabe quién es
                aplicarPermisos("sin-conexion");
            }
        }
    }


    // ---------- Permisos por rol ----------

    const PAGINA_SUSCRIPCION = "suscripcion.html";
    const PAGINA_ADMIN = "admin.html";

    // null = invitado; objeto = cuenta validada; "sin-conexion" = no se pudo verificar
    let cuentaActual = null;

    function esAdmin(cuenta) {
        return !!cuenta && typeof cuenta === "object" && cuenta.rol === "ADMIN";
    }

    function tienePremium(cuenta) {
        return !!cuenta && typeof cuenta === "object" && (cuenta.accesoPremium === true || cuenta.rol === "ADMIN");
    }

    // Crea (o reutiliza) el aviso que tapa una sección bloqueada
    function bloquear(contenedor, texto, textoBoton, accion) {
        let aviso = contenedor.querySelector(":scope > .sesion-bloqueo");
        if (!aviso) {
            aviso = document.createElement("div");
            aviso.className = "sesion-bloqueo";
            aviso.innerHTML = '<span class="sesion-bloqueo-icono" aria-hidden="true">&#128274;</span>'
                + '<p class="sesion-bloqueo-texto"></p>'
                + '<button type="button" class="sesion-bloqueo-boton"></button>';
            contenedor.classList.add("sesion-bloqueable");
            contenedor.appendChild(aviso);
        }
        aviso.querySelector(".sesion-bloqueo-texto").textContent = texto;
        const boton = aviso.querySelector(".sesion-bloqueo-boton");
        boton.hidden = !textoBoton;
        boton.textContent = textoBoton || "";
        boton.onclick = accion || null;
        aviso.hidden = false;
        contenedor.classList.add("sesion-bloqueado");
    }

    function desbloquear(contenedor) {
        const aviso = contenedor.querySelector(":scope > .sesion-bloqueo");
        if (aviso) {
            aviso.hidden = true;
        }
        contenedor.classList.remove("sesion-bloqueado");
    }

    // Chat con IA (inmobiliaria.html): cada panel que tiene mensajes del asistente
    function aplicarPermisosChat(cuenta) {
        document.querySelectorAll(".bi-panel").forEach(function (panel) {
            if (!panel.querySelector(".ai-chat-messages")) {
                return;
            }
            if (cuenta === "sin-conexion") {
                bloquear(panel, "No se pudo verificar tu sesión. Revisa que los microservicios estén encendidos.");
            } else if (!cuenta) {
                bloquear(panel, "Inicia sesión para usar el asistente virtual.", "Iniciar sesión", function () {
                    abrirVentana();
                });
            } else if (!tienePremium(cuenta)) {
                bloquear(panel, "Suscríbete a PREMIUM para acceder a esta función.", "Ver planes", function () {
                    window.location.href = PAGINA_SUSCRIPCION;
                });
            } else {
                desbloquear(panel);
            }
        });
    }

    // Secciones marcadas con data-requiere-sesion (por ejemplo el formulario de contacto)
    function aplicarPermisosSecciones(cuenta) {
        document.querySelectorAll("[data-requiere-sesion]").forEach(function (seccion) {
            if (cuenta && typeof cuenta === "object") {
                desbloquear(seccion);
            } else if (cuenta === "sin-conexion") {
                bloquear(seccion, "No se pudo verificar tu sesión. Revisa que los microservicios estén encendidos.");
            } else {
                bloquear(seccion, seccion.dataset.requiereSesion || "Inicia sesión para acceder a esta sección.",
                    "Iniciar sesión", function () {
                        abrirVentana();
                    });
            }
        });
    }

    // Enlaces del menú a páginas que piden sesión: el invitado ve la ventana de login
    function alIrAPaginaProtegida(evento) {
        if (cuentaActual && typeof cuentaActual === "object") {
            return; // con sesión, el enlace funciona normal
        }
        evento.preventDefault();
        abrirVentana(this.getAttribute("href"));
    }

    // Pestaña "Panel de admin" junto al logo, solo para ADMIN
    function aplicarPestanaAdmin(cuenta) {
        let pestana = document.querySelector("header .nav-admin");
        if (!esAdmin(cuenta)) {
            if (pestana) {
                pestana.remove();
            }
            return;
        }
        if (pestana) {
            return;
        }
        const logo = document.querySelector("header .logo");
        if (!logo) {
            return;
        }
        pestana = document.createElement("a");
        pestana.href = PAGINA_ADMIN;
        pestana.className = "nav-admin";
        pestana.textContent = "Panel de admin";
        if (/(^|\/)admin\.html$/.test(window.location.pathname)) {
            pestana.classList.add("active");
        }
        logo.insertAdjacentElement("afterend", pestana);
    }

    function aplicarPermisos(cuenta) {
        cuentaActual = cuenta;
        window.grupo1015Sesion.cuenta = (cuenta && typeof cuenta === "object") ? cuenta : null;

        aplicarPermisosChat(cuenta);
        aplicarPermisosSecciones(cuenta);
        aplicarPestanaAdmin(cuenta);

        document.dispatchEvent(new CustomEvent("grupo1015:sesion", {
            detail: window.grupo1015Sesion.cuenta
        }));
    }

    function iniciar() {
        enlacesDeLogin().forEach(function (enlace) {
            enlace.addEventListener("click", alHacerClic);
        });
        document.querySelectorAll('nav a[href="contacto.html"]').forEach(function (enlace) {
            enlace.addEventListener("click", alIrAPaginaProtegida);
        });
        revisarSesion();
    }

    // Para que otras páginas puedan abrir la ventana: window.grupo1015Sesion.abrir()
    window.grupo1015Sesion = {
        abrir: abrirVentana,
        cerrarSesion: cerrarSesion,
        cuenta: null,
        token: leerToken
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", iniciar);
    } else {
        iniciar();
    }
})();
