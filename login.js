// login.js: lógica del formulario de login.html.
// Usa las funciones que ya existen en frontend/usuario/js/api.js:
//   iniciarSesion(correo, password) -> POST /api/auth/login  -> {token, tipo, expiraEnMinutos}
//   validarSesion(token)            -> GET  /api/auth/validar -> {cuentaId, correo, usuarioId, rol, ...}
//   guardarToken(token) / borrarToken()  (clave "grupo1015_token" en localStorage)

// A dónde se va cada rol después de iniciar sesión.
// IMPORTANTE: el panel de administración y el panel personal TODAVÍA NO EXISTEN.
// Por ahora ambos van a frontend/usuario/index.html, que ya muestra el mensaje según el rol.
// Cuando se creen los paneles, solo se cambian estas dos líneas.
const DESTINO_POR_ROL = {
    ADMIN: "frontend/usuario/index.html",   // futuro: panel de administración
    USUARIO: "frontend/usuario/index.html"  // futuro: panel personal
};

const formulario = document.getElementById("form-login");
const mensaje = document.getElementById("mensaje");
const boton = formulario.querySelector("button[type=submit]");

function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = "login-mensaje " + tipo; // "error" o "exito"
}

function ocultarMensaje() {
    mensaje.className = "login-mensaje oculto";
}

// Traduce el error de la API a un mensaje claro
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

formulario.addEventListener("submit", async function (evento) {
    evento.preventDefault(); // evita que la página se recargue

    const correo = formulario.correo.value.trim();
    const password = formulario.password.value;

    // Revisiones antes de enviar (el backend también valida)
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
        // 1. Enviar credenciales -> el backend responde con el JWT
        const respuesta = await iniciarSesion(correo, password);
        guardarToken(respuesta.token);

        // 2. Preguntar al backend qué rol tiene ese token
        const cuenta = await validarSesion(respuesta.token);

        // 3. Redirigir según el rol
        const destino = DESTINO_POR_ROL[cuenta.rol];
        if (!destino) {
            throw { status: -1 };
        }
        mostrarMensaje("Sesión iniciada. Redirigiendo...", "exito");
        window.location.href = destino;
    } catch (error) {
        borrarToken(); // datos incorrectos: no se deja entrar
        mostrarMensaje(
            error.status === -1 ? "Tu cuenta tiene un rol no reconocido." : textoDeError(error),
            "error"
        );
        boton.disabled = false;
        boton.textContent = "Iniciar sesión";
    }
});
