/* =====================================================================
   Conexión de la página web con los microservicios de Grupo 10.15
   - contacto.html  → Microservicio de Citas (POST /api/citas)
   - inmobiliaria.html → Microservicio de Ofertas y Remates (GET /api/ofertas)
   Si un microservicio no está disponible, la página sigue funcionando
   normal: simplemente no se muestran las ofertas o se avisa en el formulario.
   ===================================================================== */

// Cuando exista el API Gateway, solo hay que cambiar estas dos URLs.
const API_CONFIG = {
    citas: 'http://localhost:8081/api/citas',
    ofertas: 'http://localhost:8082/api/ofertas'
};

const formatoPrecio = new Intl.NumberFormat('es-MX', {
    style: 'currency', currency: 'MXN', maximumFractionDigits: 0
});

const formatoFecha = new Intl.DateTimeFormat('es-MX', {
    day: 'numeric', month: 'long', year: 'numeric'
});

// Evita que texto guardado en la base de datos se interprete como HTML
function escaparHTML(texto) {
    return String(texto ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

/* =====================================================================
   1. CITAS — formulario de contacto.html
   ===================================================================== */

const NOMBRES_CAMPOS = {
    nombre: 'Nombre', email: 'Correo electrónico', telefono: 'Teléfono',
    propiedad: 'Propiedad o servicio', fechaHora: 'Fecha y hora', mensaje: 'Mensaje'
};

function mostrarEstado(elemento, tipo, html) {
    if (!elemento) return;
    elemento.className = 'form-estado' + (tipo ? ' form-estado--' + tipo : '');
    elemento.innerHTML = html;
    elemento.style.display = html ? 'block' : 'none';
}

function iniciarFormularioCitas() {
    const form = document.getElementById('form-cita');
    if (!form) return;

    const estado = document.getElementById('form-cita-estado');
    const boton = form.querySelector('button[type="submit"]');

    // Si se llega desde una propiedad (contacto.html?propiedad=chapala), se preselecciona
    const propiedadURL = new URLSearchParams(location.search).get('propiedad');
    if (propiedadURL && [...form.propiedad.options].some(o => o.value === propiedadURL)) {
        form.propiedad.value = propiedadURL;
    }

    // No permitir elegir fechas pasadas
    const ahora = new Date();
    ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset());
    form.fechaHora.min = ahora.toISOString().slice(0, 16);

    form.addEventListener('submit', async (evento) => {
        evento.preventDefault();

        const datos = {
            nombre: form.nombre.value.trim(),
            email: form.email.value.trim(),
            telefono: form.telefono.value.trim() || null,
            propiedad: form.propiedad.value || null,
            fechaHora: form.fechaHora.value || null,
            mensaje: form.mensaje.value.trim() || null
        };

        const textoBoton = boton.textContent;
        boton.disabled = true;
        boton.textContent = 'Enviando...';
        mostrarEstado(estado, '', '');

        try {
            const respuesta = await fetch(API_CONFIG.citas, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(datos)
            });

            if (respuesta.status === 201) {
                const cita = await respuesta.json();
                form.reset();
                mostrarEstado(estado, 'ok',
                    `¡Gracias, ${escaparHTML(cita.nombre)}! Recibimos tu solicitud ` +
                    `(folio <strong>#${cita.id}</strong>). Te contactaremos pronto para confirmar.`);
            } else if (respuesta.status === 400) {
                const errores = await respuesta.json();
                const lista = Object.entries(errores)
                    .map(([campo, msg]) => `${escaparHTML(NOMBRES_CAMPOS[campo] || campo)}: ${escaparHTML(msg)}`)
                    .join('<br>');
                mostrarEstado(estado, 'error', 'Revisa los datos:<br>' + lista);
            } else {
                throw new Error('Respuesta inesperada: ' + respuesta.status);
            }
        } catch (error) {
            console.error('Error al enviar la cita:', error);
            mostrarEstado(estado, 'error',
                'No pudimos enviar tu solicitud en este momento. Intenta más tarde o ' +
                'escríbenos por WhatsApp al <a href="https://wa.me/523325239705" target="_blank" rel="noopener">+52 33 2523 9705</a>.');
        } finally {
            boton.disabled = false;
            boton.textContent = textoBoton;
        }
    });
}

/* =====================================================================
   2. OFERTAS Y REMATES — inmobiliaria.html
   ===================================================================== */

let ofertasVigentes = [];

function nombrePropiedad(id) {
    const titulo = document.querySelector(`#view-${CSS.escape(id)} h2`);
    return titulo ? titulo.textContent.trim() : id;
}

function imagenPropiedad(id) {
    const img = document.querySelector(`#view-${CSS.escape(id)} .detail-hero img`);
    return img ? img.getAttribute('src') : null;
}

function existeVista(id) {
    return !!document.getElementById('view-' + id);
}

function descuento(oferta) {
    const antes = Number(oferta.precioOriginal);
    const ahora = Number(oferta.precioFinal);
    return antes > 0 ? Math.round((1 - ahora / antes) * 100) : 0;
}

function textoVigencia(oferta) {
    if (!oferta.fechaFin) return '';
    // Se agrega la hora para que no se recorra un día por la zona horaria
    return 'Vigente hasta el ' + formatoFecha.format(new Date(oferta.fechaFin + 'T12:00:00'));
}

async function cargarOfertas() {
    if (!document.getElementById('inmo-landing')) return; // solo en inmobiliaria.html

    try {
        const respuesta = await fetch(API_CONFIG.ofertas);
        if (!respuesta.ok) throw new Error('HTTP ' + respuesta.status);
        const hoy = new Date().toISOString().slice(0, 10);
        ofertasVigentes = (await respuesta.json()).filter(o =>
            (!o.fechaInicio || o.fechaInicio <= hoy) && (!o.fechaFin || o.fechaFin >= hoy));
    } catch (error) {
        console.warn('Microservicio de ofertas no disponible:', error.message);
        ofertasVigentes = [];
    }

    const seccion = document.getElementById('ofertas-remates');
    if (seccion && ofertasVigentes.length > 0) {
        seccion.style.display = 'block';
        pintarOfertas('TODAS');
        document.querySelectorAll('.ofertas-filtro').forEach(boton => {
            boton.addEventListener('click', () => {
                document.querySelectorAll('.ofertas-filtro').forEach(b => b.classList.remove('activo'));
                boton.classList.add('activo');
                pintarOfertas(boton.dataset.filtro);
            });
        });
    }

    marcarTarjetasCatalogo();
}

function pintarOfertas(filtro) {
    const grid = document.getElementById('ofertas-grid');
    if (!grid) return;

    const lista = filtro === 'TODAS' ? ofertasVigentes : ofertasVigentes.filter(o => o.tipo === filtro);

    if (lista.length === 0) {
        grid.innerHTML = '<p class="ofertas-vacio">Por ahora no hay publicaciones en esta categoría.</p>';
        return;
    }

    grid.innerHTML = lista.map(o => {
        const img = imagenPropiedad(o.propiedad);
        const esRemate = o.tipo === 'REMATE';
        const pct = descuento(o);
        return `
            <article class="oferta-card${existeVista(o.propiedad) ? ' oferta-card--link' : ''}" data-propiedad="${escaparHTML(o.propiedad)}">
                <div class="oferta-img">
                    ${img ? `<img src="${escaparHTML(img)}" alt="${escaparHTML(nombrePropiedad(o.propiedad))}">` : ''}
                    <span class="oferta-badge ${esRemate ? 'oferta-badge--remate' : ''}">${esRemate ? 'Remate' : 'Oferta'}</span>
                    ${pct > 0 ? `<span class="oferta-pct">-${pct}%</span>` : ''}
                </div>
                <div class="oferta-body">
                    <span class="oferta-prop">${escaparHTML(nombrePropiedad(o.propiedad))}</span>
                    <h3>${escaparHTML(o.titulo)}</h3>
                    ${o.descripcion ? `<p>${escaparHTML(o.descripcion)}</p>` : ''}
                    <div class="oferta-precios">
                        <span class="oferta-antes">${formatoPrecio.format(o.precioOriginal)}</span>
                        <span class="oferta-ahora">${formatoPrecio.format(o.precioFinal)}</span>
                    </div>
                    ${textoVigencia(o) ? `<span class="oferta-vigencia">${textoVigencia(o)}</span>` : ''}
                </div>
            </article>`;
    }).join('');

    grid.querySelectorAll('.oferta-card--link').forEach(tarjeta => {
        tarjeta.addEventListener('click', () => showProject(tarjeta.dataset.propiedad));
    });
}

// Agrega una etiqueta "Oferta" o "Remate" a las tarjetas del catálogo
function marcarTarjetasCatalogo() {
    document.querySelectorAll('.project-card[onclick]').forEach(tarjeta => {
        const coincidencia = tarjeta.getAttribute('onclick').match(/showProject\('([^']+)'\)/);
        if (!coincidencia) return;
        const oferta = ofertasVigentes.find(o => o.propiedad === coincidencia[1]);
        const contenedor = tarjeta.querySelector('.img-holder');
        if (!oferta || !contenedor || contenedor.querySelector('.catalogo-badge')) return;
        const esRemate = oferta.tipo === 'REMATE';
        contenedor.insertAdjacentHTML('beforeend',
            `<span class="catalogo-badge ${esRemate ? 'oferta-badge--remate' : ''}">${esRemate ? 'Remate' : 'Oferta'}</span>`);
    });
}

// Dentro de cada propiedad: muestra la oferta (si hay) y el botón "Agendar visita"
function insertarPanelPropiedad(id) {
    const vista = document.getElementById('view-' + id);
    if (!vista) return;

    vista.querySelectorAll('.panel-micro').forEach(p => p.remove());

    const oferta = ofertasVigentes.find(o => o.propiedad === id);
    const panel = document.createElement('div');
    panel.className = 'panel-micro';
    panel.innerHTML = `
        ${oferta ? `
            <div class="panel-micro-oferta">
                <span class="oferta-badge ${oferta.tipo === 'REMATE' ? 'oferta-badge--remate' : ''}">${oferta.tipo === 'REMATE' ? 'Remate' : 'Oferta'}</span>
                <div>
                    <strong>${escaparHTML(oferta.titulo)}</strong>
                    <div class="oferta-precios">
                        <span class="oferta-antes">${formatoPrecio.format(oferta.precioOriginal)}</span>
                        <span class="oferta-ahora">${formatoPrecio.format(oferta.precioFinal)}</span>
                    </div>
                    ${textoVigencia(oferta) ? `<span class="oferta-vigencia">${textoVigencia(oferta)}</span>` : ''}
                </div>
            </div>` : ''}
        <a class="panel-micro-boton" href="contacto.html?propiedad=${encodeURIComponent(id)}">Agendar visita</a>`;

    const grid = vista.querySelector('.project-text-grid');
    if (grid) grid.parentNode.insertBefore(panel, grid);
    else vista.appendChild(panel);
}

// Se "envuelve" la función original showProject para agregar el panel
if (typeof showProject === 'function') {
    const showProjectOriginal = showProject;
    window.showProject = function (id) {
        showProjectOriginal(id);
        insertarPanelPropiedad(id);
    };
}

/* ===================================================================== */
document.addEventListener('DOMContentLoaded', () => {
    iniciarFormularioCitas();
    cargarOfertas();
});
