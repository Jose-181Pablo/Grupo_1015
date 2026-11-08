// admin.js: Panel de admin (admin.html).
// Hace desde la página lo mismo que la colección de Postman del microservicio autenticacion (Liz):
//   GET /api/auth/admin/cuentas            -> lista de cuentas
//   PUT /api/auth/admin/cuentas/{id}/plan  -> {"plan": "ESTANDAR" | "PREMIUM"}
//   PUT /api/auth/admin/cuentas/{id}/rol   -> {"rol": "USUARIO" | "ADMIN"}
// y lee los nombres del microservicio usuario (GET /api/usuarios).
// Quién entra lo decide sesion.js (evento "grupo1015:sesion"); además autenticación
// responde 401/403 si el token no es de un ADMIN.
(function () {
    "use strict";

    const API_URL = "http://localhost:8080/api";

    const aviso = document.getElementById("admin-aviso");
    const avisoTexto = document.getElementById("admin-aviso-texto");
    const avisoBoton = document.getElementById("admin-aviso-boton");
    const panel = document.getElementById("admin-panel");
    const mensaje = document.getElementById("admin-mensaje");
    const cuerpo = document.getElementById("admin-cuentas");
    const buscar = document.getElementById("admin-buscar");

    let cuentas = [];
    let nombres = {};      // usuarioId -> "Nombre Apellido"
    let miCuentaId = null;

    function token() {
        return window.grupo1015Sesion ? window.grupo1015Sesion.token() : null;
    }

    async function peticion(ruta, opciones) {
        const datos = Object.assign({ headers: {} }, opciones || {});
        datos.headers = Object.assign({ "Authorization": "Bearer " + token() }, datos.headers);

        let respuesta;
        try {
            respuesta = await fetch(API_URL + ruta, datos);
        } catch (e) {
            const error = new Error("No se pudo conectar con el servidor. Revisa que los microservicios estén encendidos.");
            error.status = 0;
            throw error;
        }

        let json = null;
        try {
            json = await respuesta.json();
        } catch (e) {
            json = null;
        }

        if (!respuesta.ok) {
            const error = new Error((json && json.error) || ("Error " + respuesta.status));
            error.status = respuesta.status;
            throw error;
        }
        return json;
    }

    function mostrarMensaje(texto, tipo) {
        mensaje.textContent = texto;
        mensaje.className = "admin-mensaje " + (tipo || "");
        mensaje.hidden = false;
    }

    function mostrarAviso(texto, conBoton) {
        panel.hidden = true;
        aviso.hidden = false;
        avisoTexto.textContent = texto;
        avisoBoton.hidden = !conBoton;
    }

    avisoBoton.addEventListener("click", function () {
        if (window.grupo1015Sesion) {
            window.grupo1015Sesion.abrir();
        }
    });

    // ---------- Datos ----------

    async function cargar() {
        try {
            cuentas = await peticion("/auth/admin/cuentas");
        } catch (error) {
            if (error.status === 401 || error.status === 403) {
                mostrarAviso("Solo los administradores pueden ver este panel.", false);
            } else {
                mostrarMensaje(error.message, "error");
            }
            return;
        }

        // Los nombres son opcionales: si el microservicio usuario no responde, se muestra "—"
        try {
            const usuarios = await peticion("/usuarios");
            nombres = {};
            (usuarios || []).forEach(function (u) {
                nombres[u.id] = [u.nombre, u.apellido].filter(Boolean).join(" ");
            });
        } catch (e) {
            nombres = {};
        }

        pintar();
    }

    function pintar() {
        const filtro = buscar.value.trim().toLowerCase();
        cuerpo.replaceChildren();

        cuentas
            .filter(function (c) {
                const texto = (c.correo + " " + (nombres[c.usuarioId] || "")).toLowerCase();
                return !filtro || texto.includes(filtro);
            })
            .forEach(function (c) {
                cuerpo.appendChild(fila(c));
            });

        document.getElementById("dato-total").textContent = cuentas.length;
        document.getElementById("dato-admins").textContent = cuentas.filter(esAdmin).length;
        document.getElementById("dato-premium").textContent = cuentas.filter(tienePremium).length;
    }

    function esAdmin(c) {
        return c.rol === "ADMIN";
    }

    function tienePremium(c) {
        return c.plan === "PREMIUM" || c.rol === "ADMIN";
    }

    function celda(texto) {
        const td = document.createElement("td");
        td.textContent = texto;
        return td;
    }

    function selector(opciones, valor, etiqueta) {
        const select = document.createElement("select");
        select.setAttribute("aria-label", etiqueta);
        opciones.forEach(function (o) {
            const opcion = document.createElement("option");
            opcion.value = o;
            opcion.textContent = o;
            opcion.selected = o === valor;
            select.appendChild(opcion);
        });
        return select;
    }

    function fila(c) {
        const tr = document.createElement("tr");
        tr.appendChild(celda(c.id));

        const correo = celda(c.correo);
        if (c.id === miCuentaId) {
            const yo = document.createElement("span");
            yo.className = "admin-yo";
            yo.textContent = " (tú)";
            correo.appendChild(yo);
        }
        tr.appendChild(correo);
        tr.appendChild(celda(nombres[c.usuarioId] || "—"));

        // Rol
        const tdRol = document.createElement("td");
        const rol = selector(["USUARIO", "ADMIN"], c.rol, "Rol de " + c.correo);
        rol.addEventListener("change", function () {
            cambiar(c, "rol", rol.value, rol);
        });
        tdRol.appendChild(rol);
        tr.appendChild(tdRol);

        // Plan
        const tdPlan = document.createElement("td");
        const plan = selector(["ESTANDAR", "PREMIUM"], c.plan, "Plan de " + c.correo);
        plan.addEventListener("change", function () {
            cambiar(c, "plan", plan.value, plan);
        });
        tdPlan.appendChild(plan);
        tr.appendChild(tdPlan);

        // Acceso premium (el ADMIN siempre lo tiene)
        const tdAcceso = document.createElement("td");
        const etiqueta = document.createElement("span");
        etiqueta.className = "admin-premium" + (tienePremium(c) ? "" : " no");
        etiqueta.textContent = tienePremium(c) ? (esAdmin(c) ? "Sí (admin)" : "Sí") : "No";
        tdAcceso.appendChild(etiqueta);
        tr.appendChild(tdAcceso);

        return tr;
    }

    async function cambiar(cuenta, campo, valor, select) {
        const anterior = cuenta[campo];
        let pregunta = "¿Cambiar el " + campo + " de " + cuenta.correo + " a " + valor + "?";
        if (campo === "rol" && cuenta.id === miCuentaId && valor !== "ADMIN") {
            pregunta += "\n\nOjo: es tu propia cuenta y ya no podrás entrar a este panel.";
        }
        if (!window.confirm(pregunta)) {
            select.value = anterior;
            return;
        }

        select.disabled = true;
        try {
            const actualizada = await peticion("/auth/admin/cuentas/" + cuenta.id + "/" + campo, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ [campo]: valor })
            });
            Object.assign(cuenta, actualizada);
            mostrarMensaje("Listo: " + cuenta.correo + " ahora tiene " + campo + " " + valor + ".", "exito");

            if (campo === "rol" && cuenta.id === miCuentaId && valor !== "ADMIN") {
                window.location.reload();
                return;
            }
            pintar();
        } catch (error) {
            select.value = anterior;
            select.disabled = false;
            mostrarMensaje(error.message, "error");
        }
    }

    buscar.addEventListener("input", pintar);
    document.getElementById("admin-recargar").addEventListener("click", function () {
        mensaje.hidden = true;
        cargar();
    });

    // ---------- Quién entra ----------

    document.addEventListener("grupo1015:sesion", function (evento) {
        const cuenta = evento.detail;
        if (!cuenta) {
            if (token()) {
                mostrarAviso("No se pudo verificar tu sesión. Revisa que los microservicios estén encendidos.", false);
            } else {
                mostrarAviso("Inicia sesión con una cuenta de administrador para ver este panel.", true);
            }
            return;
        }
        if (cuenta.rol !== "ADMIN") {
            mostrarAviso("Solo los administradores pueden ver este panel.", false);
            return;
        }

        miCuentaId = cuenta.cuentaId;
        aviso.hidden = true;
        panel.hidden = false;
        cargar();
    });
})();
