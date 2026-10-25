# Microservicio de Suscripciones

Microservicio desarrollado con Spring Boot que administra las suscripciones PREMIUM de Grupo 10.15. Cuando una persona contrata un plan, guarda la suscripción y pone su cuenta en PREMIUM en el microservicio de autenticación; con eso el chat con IA le da acceso.

## Tecnologías utilizadas

- Java 25
- Spring Boot 4
- Maven
- Spring Data JPA
- Lombok
- MariaDB
- Docker
- Docker Compose
- Postman

## Funcionalidades

- Consultar los planes disponibles.
- Contratar una suscripción (el pago por ahora es simulado).
- Consultar la suscripción activa y el historial.
- Cancelar la suscripción.
- Vencer automáticamente las suscripciones cuya fecha de fin ya pasó.
- Cambiar el plan de la cuenta (PREMIUM / ESTANDAR) en el microservicio de autenticación.
- Validar la información recibida y manejar errores de la API.
- Ejecutar el servicio y la base de datos con Docker.

## Microservicios que necesita

| Microservicio | Rama | Para qué |
|---|---|---|
| autenticacion (8082) | `liz` | Identificar a la persona con su token y cambiar el plan de su cuenta |
| usuario (8081) | `liz` | Lo usa autenticación para registrar cuentas |

## Estructura principal

```text
src/main/java/mx/grupo1015/suscripcion
├── client
├── config
├── controller
├── dto
├── exception
├── model
├── repository
├── service
└── SuscripcionServiceApplication.java
```

## Variables de entorno

Antes de ejecutar el proyecto, copia `.env.example` como `.env` y llena los valores:

```bash
cp .env.example .env
```

```env
DB_PASSWORD=contraseña_de_la_base
DB_ROOT_PASSWORD=contraseña_del_usuario_root
AUTENTICACION_URL=http://autenticacion:8082
AUTENTICACION_CORREO=suscripcion@grupo1015.com
AUTENTICACION_PASSWORD=contraseña_de_la_cuenta_de_servicio
```

El archivo `.env` contiene información privada y no debe subirse al repositorio.

### Cuenta de servicio

Para cambiar el plan de una cuenta, autenticación pide un token de ADMIN. Este microservicio inicia sesión con la cuenta de `AUTENTICACION_CORREO` y `AUTENTICACION_PASSWORD`, guarda el token y vuelve a iniciar sesión cuando vence.

1. Registra la cuenta con `POST /api/auth/registro` en autenticación.
2. Con el ADMIN inicial, dale rol ADMIN con `PUT /api/auth/admin/cuentas/{id}/rol` y el cuerpo `{"rol": "ADMIN"}`.

Para pruebas en tu equipo también puedes usar el ADMIN inicial de autenticación (`ADMIN_CORREO` y `ADMIN_PASSWORD` de su `.env`).

## Ejecución con Docker

Es necesario tener Docker Desktop instalado y ejecutándose. Primero levanta usuario y autenticación (rama `liz`); los tres comparten la red `grupo1015`.

Desde la carpeta `suscripcion-service`, construir e iniciar los contenedores:

```bash
docker compose up --build -d
```

Comprobar el estado del microservicio:

```text
http://localhost:8088/actuator/health
```

Detener los contenedores:

```bash
docker compose down
```

Si autenticación corre en IntelliJ y no en Docker, usa `AUTENTICACION_URL=http://host.docker.internal:8082`. Desde IntelliJ este microservicio usa `http://localhost:8082`.

## Puertos

| Servicio | Puerto |
|---|---:|
| Suscripciones | 8088 |
| MariaDB de Docker | 3311 |

El puerto de la aplicación se puede cambiar con la variable `PORT`. La tabla con los puertos de todos los microservicios del equipo está en [`../PUERTOS.md`](../PUERTOS.md).

## Endpoints principales

Todas las rutas de `/api/v1/suscripciones` necesitan el token que da autenticación al iniciar sesión:

```http
Authorization: Bearer <token>
```

### Ver planes

```http
GET /api/v1/planes
```

Al arrancar por primera vez se crean dos planes:

| Código | Precio | Duración |
|---|---:|---:|
| `PREMIUM_MENSUAL` | $149.00 | 30 días |
| `PREMIUM_ANUAL` | $1,490.00 | 365 días |

### Contratar suscripción

```http
POST /api/v1/suscripciones
```

```json
{
  "planCodigo": "PREMIUM_MENSUAL"
}
```

Respuesta `201` con la suscripción. La cuenta queda en PREMIUM y `GET /api/auth/validar` regresa `accesoPremium: true`.

### Suscripción activa

```http
GET /api/v1/suscripciones/actual
```

### Historial

```http
GET /api/v1/suscripciones
```

### Cancelar suscripción

```http
POST /api/v1/suscripciones/actual/cancelar
```

La suscripción queda `CANCELADA` y la cuenta regresa a ESTANDAR de inmediato.

### Respuestas de error

| Código | Cuándo |
|---:|---|
| 400 | Faltan datos en la petición |
| 401 | No hay token o no es válido |
| 404 | El plan no existe o no hay suscripción activa |
| 409 | Ya hay una suscripción activa |
| 503 | Autenticación no responde o la cuenta de servicio no es ADMIN |

## Vencimiento

Cada hora (variable `REVISION_VENCIDAS_MS`) se buscan las suscripciones activas cuya fecha de fin ya pasó. Se marcan como `VENCIDA` y la cuenta regresa a ESTANDAR. Si autenticación no responde, se intenta otra vez en la siguiente revisión.

## Pruebas

Las pruebas usan una base H2 en memoria (perfil `test`) y simulan el microservicio de autenticación, así que no necesitan Docker:

```bash
./mvnw test
```

## Postman

La colección `postman/suscripcion-service.postman_collection.json` trae todas las peticiones. Impórtala en Postman, cambia el correo y la contraseña de "Iniciar sesión" por los de una cuenta registrada y ejecútalas en orden: esa petición guarda el token en la variable `token` que usan las demás.

## Base de datos

Docker crea una base MariaDB llamada:

```text
suscripcion_db
```

Las tablas principales son:

- `planes`
- `suscripciones`

## Mejoras futuras

- Conectar un procesador de pagos real (por ahora el pago es simulado).
- Registro en Eureka.
- Botón de suscripción en la interfaz web.
