// admin-ofertas.js: apartado "Ofertas y remates" del Panel de admin (admin.html).
// Usa el microservicio de ofertas (Génesis) por el Gateway:
//   GET    /api/ofertas                     -> ofertas y remates activos
//   POST   /api/ofertas                     -> crear       (solo ADMIN)
//   PUT    /api/ofertas/{id}                -> editar      (solo ADMIN)
//   PATCH  /api/ofertas/{id}/desactivar     -> desactivar  (solo ADMIN)
//   DELETE /api/ofertas/{id}                -> borrar      (solo ADMIN)
// El precio original se sugiere con el catálogo de Pablo (/api/inmuebles o /api/terrenos).
(function () {
    "use strict";

    const API_URL = "http://localhost:8080/api";

    // Mismas claves que usan inmobiliaria.html, el chat y el microservicio de ofertas
    const INMUEBLES = {
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

    const form = document.getElementById("oferta-form");
    if (!form) {
        return;
    }

    const titulo = document.getElementById("oferta-form-titulo");
    const botonGuardar = document.getElementById("oferta-guardar");
    const botonCancelar = document.getElementById("oferta-cancelar");
    const mensaje = document.getElementById("oferta-mensaje");
    const cuerpo = document.getElementById("ofertas-lista");
    const precioAyuda = document.getElementById("oferta-precio-ayuda");

    const formatoPrecio = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN", maximumFractionDigits: 0 });

    let ofertas = [];
    let editando = null;   // id de la oferta que se está editando

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
            const error = new Error("No se pudo conectar con el microservicio de ofertas. Revisa que esté encendido.");
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
            // Errores de validación: {"campo": "mensaje"}; los demás: {"error": "..."}
            let texto = json && json.error;
            if (!texto && json && typeof json === "object") {
                texto = Object.keys(json).map(function (k) { return k + ": " + json[k]; }).join(". ");
            }
            const error = new Error(texto || ("Error " + respuesta.status));
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

    // ---------- Formulario ----------

    const selectInmueble = form.elements.propiedad;
    Object.keys(INMUEBLES).forEach(function (clave) {
        const opcion = document.createElement("option");
        opcion.value = clave;
        opcion.textContent = INMUEBLES[clave];
        selectInmueble.appendChild(opcion);
    });

    function hoy() {
        const d = new Date();
        d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
        return d.toISOString().slice(0, 10);
    }

    function limpiarFormulario() {
        editando = null;
        tituloSugerido = "";
        form.reset();
        form.elements.fechaInicio.value = hoy();
        titulo.textContent = "Nueva oferta o remate";
        botonGuardar.textContent = "Publicar";
        botonCancelar.hidden = true;
        precioAyuda.textContent = "";
    }

    // Título sugerido ("Casa 65 en oferta"); solo se cambia si el admin no escribió otro
    let tituloSugerido = "";
    function sugerirTitulo() {
        const clave = selectInmueble.value;
        const actual = form.elements.titulo.value;
        if (!clave || (actual && actual !== tituloSugerido)) {
            return;
        }
        tituloSugerido = INMUEBLES[clave] + " en " +
            (form.elements.tipo.value === "REMATE" ? "remate" : "oferta");
        form.elements.titulo.value = tituloSugerido;
    }
    form.elements.tipo.addEventListener("change", sugerirTitulo);

    // Al elegir un inmueble se sugiere su precio del catálogo (si el catálogo responde)
    selectInmueble.addEventListener("change", async function () {
        const clave = selectInmueble.value;
        precioAyuda.textContent = "";
        if (!clave) {
            return;
        }
        sugerirTitulo();
        const precio = await precioCatalogo(clave);
        if (precio && selectInmueble.value === clave) {
            precioAyuda.textContent = "Precio en catálogo: " + formatoPrecio.format(precio);
            if (!form.elements.precioOriginal.value) {
                form.elements.precioOriginal.value = precio;
            }
        }
    });

    async function precioCatalogo(clave) {
        const rutas = ["/inmuebles/clave/", "/terrenos/clave/"];
        for (let i = 0; i < rutas.length; i++) {
            try {
                const respuesta = await fetch(API_URL + rutas[i] + encodeURIComponent(clave));
                if (respuesta.ok) {
                    const datos = await respuesta.json();
                    if (datos && datos.precio) {
                        return Number(datos.precio);
                    }
                }
            } catch (e) {
                // Sin catálogo: el admin escribe el precio a mano
            }
        }
        return null;
    }

    form.addEventListener("submit", async function (evento) {
        evento.preventDefault();

        const datos = {
            propiedad: selectInmueble.value,
            tipo: form.elements.tipo.value,
            titulo: form.elements.titulo.value.trim(),
            descripcion: form.elements.descripcion.value.trim() || null,
            precioOriginal: Number(form.elements.precioOriginal.value),
            precioFinal: Number(form.elements.precioFinal.value),
            fechaInicio: form.elements.fechaInicio.value || null,
            fechaFin: form.elements.fechaFin.value || null
        };

        if (datos.precioFinal > datos.precioOriginal) {
            mostrarMensaje("El precio final no puede ser mayor que el precio original.", "error");
            return;
        }
        if (datos.fechaInicio && datos.fechaFin && datos.fechaFin < datos.fechaInicio) {
            mostrarMensaje("La fecha de fin no puede ser anterior a la de inicio.", "error");
            return;
        }

        botonGuardar.disabled = true;
        try {
            if (editando) {
                await peticion("/ofertas/" + editando, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(datos)
                });
                mostrarMensaje("Listo: se actualizó \"" + datos.titulo + "\".", "exito");
            } else {
                await peticion("/ofertas", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(datos)
                });
                mostrarMensaje("Listo: se publicó \"" + datos.titulo + "\" para " + INMUEBLES[datos.propiedad] + ".", "exito");
            }
            limpiarFormulario();
            await cargar();
        } catch (error) {
            mostrarMensaje(error.message, "error");
        } finally {
            botonGuardar.disabled = false;
        }
    });

    botonCancelar.addEventListener("click", limpiarFormulario);

    function editar(o) {
        editando = o.id;
        tituloSugerido = "";
        selectInmueble.value = o.propiedad;
        form.elements.tipo.value = o.tipo;
        form.elements.titulo.value = o.titulo || "";
        form.elements.descripcion.value = o.descripcion || "";
        form.elements.precioOriginal.value = o.precioOriginal;
        form.elements.precioFinal.value = o.precioFinal;
        form.elements.fechaInicio.value = o.fechaInicio || "";
        form.elements.fechaFin.value = o.fechaFin || "";
        titulo.textContent = "Editar: " + o.titulo;
        botonGuardar.textContent = "Guardar cambios";
        botonCancelar.hidden = false;
        precioAyuda.textContent = "";
        form.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    // ---------- Lista ----------

    async function cargar() {
        try {
            ofertas = await peticion("/ofertas");
        } catch (error) {
            ofertas = [];
            mostrarMensaje(error.message, "error");
        }
        pintar();
    }

    function nombreInmueble(clave) {
        return INMUEBLES[clave] || clave;
    }

    function celda(texto) {
        const td = document.createElement("td");
        td.textContent = texto;
        return td;
    }

    function boton(texto, clase, accion) {
        const b = document.createElement("button");
        b.type = "button";
        b.className = "admin-accion " + (clase || "");
        b.textContent = texto;
        b.addEventListener("click", accion);
        return b;
    }

    function vigencia(o) {
        const desde = o.fechaInicio || "—";
        const hasta = o.fechaFin || "sin fecha de fin";
        return desde + " a " + hasta;
    }

    function pintar() {
        cuerpo.replaceChildren();
        document.getElementById("dato-ofertas").textContent = ofertas.filter(function (o) { return o.tipo === "OFERTA"; }).length;
        document.getElementById("dato-remates").textContent = ofertas.filter(function (o) { return o.tipo === "REMATE"; }).length;

        if (ofertas.length === 0) {
            const tr = document.createElement("tr");
            const td = celda("No hay ofertas ni remates activos.");
            td.colSpan = 6;
            tr.appendChild(td);
            cuerpo.appendChild(tr);
            return;
        }

        ofertas.forEach(function (o) {
            const tr = document.createElement("tr");
            tr.appendChild(celda(nombreInmueble(o.propiedad)));

            const tdTipo = document.createElement("td");
            const etiqueta = document.createElement("span");
            etiqueta.className = "admin-tipo" + (o.tipo === "REMATE" ? " remate" : "");
            etiqueta.textContent = o.tipo === "REMATE" ? "Remate" : "Oferta";
            tdTipo.appendChild(etiqueta);
            tr.appendChild(tdTipo);

            tr.appendChild(celda(o.titulo));

            const tdPrecio = document.createElement("td");
            const antes = document.createElement("s");
            antes.textContent = formatoPrecio.format(o.precioOriginal);
            const ahora = document.createElement("strong");
            ahora.textContent = formatoPrecio.format(o.precioFinal);
            tdPrecio.append(antes, document.createElement("br"), ahora);
            tr.appendChild(tdPrecio);

            tr.appendChild(celda(vigencia(o)));

            const tdAcciones = document.createElement("td");
            tdAcciones.className = "admin-acciones";
            tdAcciones.append(
                boton("Editar", "", function () { editar(o); }),
                boton("Desactivar", "", function () { desactivar(o); }),
                boton("Borrar", "peligro", function () { borrar(o); })
            );
            tr.appendChild(tdAcciones);

            cuerpo.appendChild(tr);
        });
    }

    async function desactivar(o) {
        if (!window.confirm("¿Desactivar \"" + o.titulo + "\"? Dejará de mostrarse en la página y en el asistente virtual.")) {
            return;
        }
        try {
            await peticion("/ofertas/" + o.id + "/desactivar", { method: "PATCH" });
            mostrarMensaje("Se desactivó \"" + o.titulo + "\".", "exito");
            await cargar();
        } catch (error) {
            mostrarMensaje(error.message, "error");
        }
    }

    async function borrar(o) {
        if (!window.confirm("¿Borrar \"" + o.titulo + "\" para siempre? Esto no se puede deshacer.")) {
            return;
        }
        try {
            await peticion("/ofertas/" + o.id, { method: "DELETE" });
            if (editando === o.id) {
                limpiarFormulario();
            }
            mostrarMensaje("Se borró \"" + o.titulo + "\".", "exito");
            await cargar();
        } catch (error) {
            mostrarMensaje(error.message, "error");
        }
    }

    // ---------- Quién entra (mismo aviso que admin.js) ----------

    document.addEventListener("grupo1015:sesion", function (evento) {
        const cuenta = evento.detail;
        if (cuenta && cuenta.rol === "ADMIN") {
            limpiarFormulario();
            cargar();
        }
    });
})();
