// api.js: funciones que hablan con la API REST (no tocan el HTML).
//
// Dirección de la API. Se usa el API Gateway del equipo (puerto 8080), que reenvía
//   /api/auth/**     -> microservicio autenticacion (8082)
//   /api/usuarios/** -> microservicio usuario (8081)
// Si cambia la dirección, solo se modifica esta línea.
const API_URL = "http://localhost:8080/api";

// Misma clave que usa la página de suscripción del equipo para guardar el token.
const CLAVE_TOKEN = "grupo1015_token";

function obtenerToken() {
    return localStorage.getItem(CLAVE_TOKEN);
}

function guardarToken(token) {
    localStorage.setItem(CLAVE_TOKEN, token);
}

function borrarToken() {
    localStorage.removeItem(CLAVE_TOKEN);
}

// Hace la petición y devuelve el JSON. Si algo sale mal lanza un Error
// con "status" (código HTTP, o 0 si no hubo conexión) y "detalle" (mensaje del backend).
async function peticion(ruta, opciones = {}) {
    let respuesta;
    try {
        respuesta = await fetch(API_URL + ruta, opciones);
    } catch (e) {
        const error = new Error("Sin conexión");
        error.status = 0; // el servidor está apagado o el navegador bloqueó la petición (CORS)
        throw error;
    }

    let datos = null;
    try {
        datos = await respuesta.json();
    } catch (e) {
        datos = null; // la respuesta no trae JSON
    }

    if (!respuesta.ok) {
        const error = new Error("Error " + respuesta.status);
        error.status = respuesta.status;
        error.detalle = datos && datos.error; // el backend responde {"error": "mensaje"}
        throw error;
    }
    return datos;
}

// POST /api/auth/registro  ->  201 {id, correo, usuarioId, rol, plan}
function registrar(nombre, apellido, correo, password) {
    return peticion("/auth/registro", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nombre, apellido, correo, password })
    });
}

// POST /api/auth/login  ->  200 {token, tipo, expiraEnMinutos}
function iniciarSesion(correo, password) {
    return peticion("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ correo, password })
    });
}

// GET /api/auth/validar  ->  200 {cuentaId, correo, usuarioId, rol, plan, accesoPremium}
// Aquí se sabe si la persona es ADMIN o USUARIO.
function validarSesion(token) {
    return peticion("/auth/validar", {
        headers: { "Authorization": "Bearer " + token }
    });
}

// GET /api/usuarios/{id}  ->  200 {id, nombre, apellido, correo}
function obtenerUsuario(id) {
    return peticion("/usuarios/" + id);
}
