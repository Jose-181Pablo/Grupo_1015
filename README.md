# Grupo 10.15 · Sitio inmobiliario con microservicios

Sitio web de Grupo 10.15 (arquitectura e inmobiliaria en Guadalajara) conectado a un conjunto de microservicios en Spring Boot. Cada microservicio tiene su propia base de datos y se registra en un Eureka compartido; la página habla con todos a través de un solo API Gateway.

Esta es la rama `completo`: junta el trabajo de las ramas individuales del equipo (`aldo`, `liz`, `pablo` y `gene1`).

## Estructura

```
Grupo_1015/
├── Microservicios/            Todos los microservicios (uno por carpeta)
│   ├── docker-compose.yml     Enciende TODO con un solo comando
│   ├── PUERTOS.md             Lista de puertos del equipo
│   ├── eureka-server/         Registro de servicios (8761)
│   ├── api-gateway/           Puerta de entrada única (8080)
│   ├── usuario/               Perfiles de usuario (8081)
│   ├── autenticacion/         Login, tokens JWT, roles y planes (8082)
│   ├── inmuebles-service/     Catálogo de casas (8083)
│   ├── terrenos-service/      Catálogo de terrenos (8084)
│   ├── chat-ia-service/       Asistente virtual con Gemini (8085)
│   ├── microservicio-citas/   Citas para visitar propiedades (8086)
│   ├── microservicio-ofertas/ Ofertas y remates (8087)
│   └── suscripcion-service/   Suscripción Premium con pago simulado (8088)
├── postman/                   Colecciones de Postman de todos los microservicios
├── frontend/usuario/          Página "Tu cuenta" (registro) de Liz
├── assets/                    Imágenes del sitio
└── *.html, *.js, *.css        Páginas del sitio web
```

Cada carpeta de `Microservicios/` tiene su propio `Dockerfile` y `docker-compose.yml` (con su base de datos), así que se puede encender sola o junto con las demás.

## Microservicios y responsables

| Puerto | Microservicio | Qué hace | Base de datos | Responsable |
|---:|---|---|---|---|
| 8761 | eureka-server | Registro donde cada servicio se anota para que los demás lo encuentren | — | Aldo |
| 8080 | api-gateway | Recibe todas las peticiones de la página y las manda al servicio correcto | — | Aldo |
| 8081 | usuario | Datos personales de cada usuario | MariaDB 3308 | Liz |
| 8082 | autenticacion | Inicio de sesión, token JWT, rol (USUARIO/ADMIN) y plan (ESTANDAR/PREMIUM) | MariaDB 3309 | Liz |
| 8083 | inmuebles-service | Catálogo de casas | PostgreSQL 5433 | Pablo |
| 8084 | terrenos-service | Catálogo de terrenos | PostgreSQL 5434 | Pablo |
| 8085 | chat-ia-service | Asistente virtual: responde dudas de cada propiedad con Gemini usando el catálogo y las ofertas | MariaDB 3310 | Aldo |
| 8086 | microservicio-citas | Solicitudes de cita desde la página de contacto | MariaDB 3307 | Génesis |
| 8087 | microservicio-ofertas | Ofertas y remates de propiedades (solo un ADMIN puede crearlos o modificarlos) | MariaDB 3312 | Génesis |
| 8088 | suscripcion-service | Contratar o cancelar Premium (pago simulado); cambia el plan en autenticación | MariaDB 3311 | Aldo |

Rutas del Gateway (todas en `http://localhost:8080`): `/api/usuarios/**`, `/api/auth/**`, `/api/inmuebles/**`, `/api/terrenos/**`, `/api/v1/conversaciones/**`, `/api/citas/**`, `/api/ofertas/**`, `/api/v1/suscripciones/**` y `/api/v1/planes/**`.

## Cómo se comunican

- Todos los contenedores están en la red de Docker `grupo1015`, así se encuentran por su nombre.
- Al arrancar, cada microservicio se registra en Eureka. Cuando uno necesita a otro, le pregunta a Eureka su dirección (con una URL de respaldo en su configuración).
- La página web solo conoce al Gateway (8080); el Gateway busca en Eureka a qué servicio mandar cada petición.
- Ejemplos: el chat consulta inmuebles, terrenos y ofertas; suscripción y ofertas validan el token con autenticación; autenticación crea el perfil en usuario.

## Cómo encenderlo

Requisitos: Docker Desktop encendido.

1. Crea la clave de Gemini para el asistente virtual: copia `Microservicios/chat-ia-service/gemini.env.example` como `gemini.env` en la misma carpeta y escribe tu `GOOGLE_API_KEY`. Este archivo **no se sube** a git.
2. Desde la carpeta `Microservicios`:

   ```
   docker compose up -d --build
   ```

3. Espera a que todos aparezcan en Eureka: <http://localhost:8761>.
4. Para apagar todo: `docker compose down` (agrega `-v` para borrar también las bases de datos).

Para encender un solo microservicio, entra a su carpeta y usa el mismo comando. Usa una forma a la vez: si encendiste uno por separado, apágalo en su carpeta antes de encender todo junto.

## Cómo abrir la página

Abre `index.html` desde IntelliJ (botón del navegador), para que se sirva en `http://localhost:63342/...`. Si se abre con doble clic (`file://`), el navegador bloquea las peticiones a los microservicios.

- **Invitado:** ve el sitio, pero no puede usar el asistente virtual ni la página de contacto.
- **Usuario:** puede agendar citas; el asistente virtual requiere plan Premium (se contrata en Suscripción).
- **Admin:** tiene todo, incluido el **Panel de admin** (cambiar rol y plan de las cuentas, y crear ofertas y remates). El primer admin se crea con `ADMIN_CORREO` y `ADMIN_PASSWORD` de `Microservicios/autenticacion/.env`.

## Postman

En `postman/` hay una colección por microservicio: `usuario`, `autenticacion`, `inmuebles-terrenos`, `chat-ia-service`, `citas-ofertas` y `suscripcion-service`. `inmobiliaria-service` es la colección anterior de Pablo, de cuando inmuebles y terrenos eran un solo servicio.

Las peticiones que crean o modifican ofertas, y las de administración de cuentas, necesitan el token de una cuenta ADMIN (se obtiene con `POST /api/auth/login`).

## Reglas del repositorio

- Los `.env` sí se suben (el repositorio es privado); lo único que nunca se sube es `gemini.env`.
- Cada integrante trabaja en su rama individual; `completo` se actualiza haciendo merge de esas ramas.
