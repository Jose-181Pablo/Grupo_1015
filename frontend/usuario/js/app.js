// app.js: lógica de la pantalla (formularios, mensajes y rol).
// Usa las funciones de api.js.

const panelAcceso = document.getElementById("panel-acceso");
const panelSesion = document.getElementById("panel-sesion");
const formRegistro = document.getElementById("form-registro");
const formLogin = document.getElementById("form-login");
const mensaje = document.getElementById("mensaje");
const etiquetaRol = document.getElementById("rol-actual");

// Texto que se muestra según el rol detectado
const TEXTOS_ROL = {
    ADMIN: {
        titulo: "Hola",
        mensaje: "Iniciaste sesión como ADMIN. Tienes acceso a las funciones de administración, incluida la gestión de usuarios."
    },
    USUARIO: {
        titulo: "Hola",
        mensaje: "Iniciaste sesión como USUARIO. Ya puedes usar las funciones permitidas para tu cuenta."
    },
    INVITADO: {
        titulo: "Estás como invitado",
        mensaje: "Navegas como INVITADO: solo puedes ver las páginas públicas. Crea una cuenta para acceder a más funciones."
    }
};

// ---------- Mensajes ----------

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = "mensaje " + tipo; // "error" o "exito"
}

function ocultarMensaje() {
    mensaje.className = "mensaje oculto";
}

// Convierte un error de la API en un mensaje claro para la persona
function textoDeError(error) {
    switch (error.status) {
        case 0:
            return "Hay un error: no se pudo conectar con el servidor. Revisa que los microservicios estén encendidos.";
        case 400:
            return "Hay un error: faltan datos obligatorios o no son válidos.";
        case 401:
            return "Hay un error: el correo o la contraseña no son correctos.";
        case 409:
            return "Hay un error: ese correo ya está registrado. Inicia sesión.";
        case 503:
            return "Hay un error: el servicio de usuarios no está disponible. Intenta más tarde.";
        default:
            return "Hay un error (código " + error.status + "). Intenta de nuevo.";
    }
}

// ---------- Pantallas ----------

// Muestra el panel de sesión con el mensaje del rol
function mostrarRol(rol, cuenta, nombre) {
    const textos = TEXTOS_ROL[rol];
    etiquetaRol.textContent = rol;

    document.getElementById("sesion-titulo").textContent =
        nombre ? textos.titulo + ", " + nombre : textos.titulo;
    document.getElementById("sesion-mensaje").textContent = textos.mensaje;
    document.getElementById("sesion-correo").textContent = cuenta ? cuenta.correo : "Sin cuenta";

    // El invitado no tiene sesión que cerrar: el botón le regresa al registro
    document.getElementById("boton-salir").textContent = rol === "INVITADO" ? "Crear cuenta" : "Cerrar sesión";

    ocultarMensaje();
    panelAcceso.classList.add("oculto");
    panelSesion.classList.remove("oculto");
}

function mostrarAcceso() {
    etiquetaRol.textContent = "INVITADO";
    panelSesion.classList.add("oculto");
    panelAcceso.classList.remove("oculto");
}

// Pide a autenticacion el rol de la cuenta y lo muestra
async function detectarRol(token) {
    const cuenta = await validarSesion(token); // {correo, usuarioId, rol, ...}

    // El nombre viene del microservicio usuario. Si no responde, se saluda sin nombre.
    let nombre = "";
    try {
        const usuario = await obtenerUsuario(cuenta.usuarioId);
        nombre = usuario.nombre;
    } catch (e) {
        nombre = "";
    }

    mostrarRol(cuenta.rol, cuenta, nombre);
}

// Desactiva el botón mientras espera la respuesta, para evitar doble envío
function bloquear(formulario, bloqueado) {
    formulario.querySelector("button[type=submit]").disabled = bloqueado;
}

// ---------- Pestañas ----------

document.querySelectorAll(".pestana").forEach(function (pestana) {
    pestana.addEventListener("click", function () {
        document.querySelectorAll(".pestana").forEach(function (p) {
            p.classList.remove("activa");
        });
        pestana.classList.add("activa");

        formRegistro.classList.toggle("oculto", pestana.dataset.formulario !== "form-registro");
        formLogin.classList.toggle("oculto", pestana.dataset.formulario !== "form-login");
        ocultarMensaje();
    });
});

// ---------- Registro ----------

formRegistro.addEventListener("submit", async function (evento) {
    evento.preventDefault(); // evita que el navegador recargue la página

    const nombre = formRegistro.nombre.value.trim();
    const apellido = formRegistro.apellido.value.trim();
    const correo = formRegistro.correo.value.trim();
    const password = formRegistro.password.value;
    const confirmar = formRegistro.confirmar.value;

    // Revisiones antes de enviar (el backend también valida)
    if (!nombre || !apellido || !correo || !password) {
        mostrarMensaje("Hay un error: completa todos los campos.", "error");
        return;
    }
    if (!formRegistro.correo.checkValidity()) {
        mostrarMensaje("Hay un error: escribe un correo válido.", "error");
        return;
    }
    if (password !== confirmar) {
        mostrarMensaje("Hay un error: las contraseñas no coinciden.", "error");
        return;
    }

    bloquear(formRegistro, true);
    try {
        await registrar(nombre, apellido, correo, password);
        // Con la cuenta creada, se inicia sesión para obtener el token y detectar el rol
        const respuesta = await iniciarSesion(correo, password);
        guardarToken(respuesta.token);
        formRegistro.reset();
        await detectarRol(respuesta.token);
    } catch (error) {
        mostrarMensaje(textoDeError(error), "error");
    } finally {
        bloquear(formRegistro, false);
    }
});

// ---------- Inicio de sesión ----------

formLogin.addEventListener("submit", async function (evento) {
    evento.preventDefault();

    const correo = formLogin.correo.value.trim();
    const password = formLogin.password.value;

    if (!correo || !password) {
        mostrarMensaje("Hay un error: escribe tu correo y tu contraseña.", "error");
        return;
    }

    bloquear(formLogin, true);
    try {
        const respuesta = await iniciarSesion(correo, password);
        guardarToken(respuesta.token);
        formLogin.reset();
        await detectarRol(respuesta.token);
    } catch (error) {
        // Si los datos no son correctos (401) no se deja entrar
        borrarToken();
        mostrarMensaje(textoDeError(error), "error");
    } finally {
        bloquear(formLogin, false);
    }
});

// ---------- Invitado y cerrar sesión ----------

document.getElementById("boton-invitado").addEventListener("click", function () {
    borrarToken();
    mostrarRol("INVITADO", null, "");
});

document.getElementById("boton-salir").addEventListener("click", function () {
    borrarToken();
    mostrarAcceso();
});

// ---------- Al abrir la página ----------
// Si ya hay un token guardado, se revisa si sigue siendo válido.

(async function iniciar() {
    const token = obtenerToken();
    if (!token) {
        return; // sin token = INVITADO, se queda el formulario
    }
    try {
        await detectarRol(token);
    } catch (error) {
        borrarToken(); // token vencido o inválido
        if (error.status === 0) {
            mostrarMensaje(textoDeError(error), "error");
        }
    }
})();
