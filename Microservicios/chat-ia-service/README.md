# Microservicio de Chat con IA

Microservicio desarrollado con Spring Boot que permite crear conversaciones relacionadas con propiedades inmobiliarias, almacenar mensajes y generar respuestas mediante la API de Gemini.

## Tecnologías utilizadas

- Java 25
- Spring Boot 4
- Maven
- Spring Data JPA
- Lombok
- MariaDB
- Gemini API
- Docker
- Docker Compose
- Postman

## Funcionalidades

- Crear conversaciones.
- Consultar conversaciones.
- Enviar mensajes.
- Generar respuestas con Gemini.
- Guardar el historial en MariaDB.
- Eliminar conversaciones.
- Validar la información recibida.
- Manejar errores de la API.
- Ejecutar el servicio y la base de datos con Docker.

## Estructura principal

```text
src/main/java/mx/grupo1015/chatia
├── controller
├── dto
├── exception
├── model
├── repository
├── service
└── ChatIaServiceApplication.java
```

## Variables de entorno

Antes de ejecutar el proyecto, copia `.env.example` como `.env` y llena los valores:

```bash
cp .env.example .env
```

```env
DB_PASSWORD=contraseña_de_la_base
DB_ROOT_PASSWORD=contraseña_del_usuario_root
GOOGLE_API_KEY=clave_de_gemini
GEMINI_MODEL=gemini-flash-latest
```

El archivo `.env` contiene información privada y no debe subirse al repositorio.

## Ejecución con Docker

Es necesario tener Docker Desktop instalado y ejecutándose.

Desde la carpeta `chat-ia-service`, construir e iniciar los contenedores:

```bash
docker compose up --build -d
```

Consultar su estado:

```bash
docker compose ps
```

Comprobar el estado del microservicio:

```text
http://localhost:8085/actuator/health
```

Detener los contenedores:

```bash
docker compose down
```

Para eliminar también la base de datos almacenada en Docker:

```bash
docker compose down -v
```

> El parámetro `-v` elimina permanentemente los datos almacenados en el volumen de MariaDB.

## Puertos

| Servicio | Puerto |
|---|---:|
| Chat con IA | 8085 |
| MariaDB de Docker | 3310 |

El puerto de la aplicación se puede cambiar con la variable `PORT`. La tabla con los puertos de todos los microservicios del equipo está en [`../PUERTOS.md`](../PUERTOS.md).

## Endpoints principales

### Crear conversación

```http
POST /api/v1/conversaciones
```

Ejemplo:

```json
{
  "usuarioId": "usuario-1",
  "propiedadId": "lago_nogal",
  "titulo": "Consulta sobre Lago Nogal"
}
```

### Enviar mensaje

```http
POST /api/v1/conversaciones/{conversacionId}/mensajes
```

Ejemplo:

```json
{
  "pregunta": "¿Qué información tienes disponible sobre esta propiedad?"
}
```

### Consultar conversación

```http
GET /api/v1/conversaciones/{conversacionId}
```

### Eliminar conversación

```http
DELETE /api/v1/conversaciones/{conversacionId}
```

## Integración con el catálogo

Antes de llamar a Gemini, el chat consulta los microservicios de catálogo de la rama `pablo`:

| Microservicio | Puerto | Endpoint |
|---|---:|---|
| `inmuebles-service` (casas) | 8083 | `GET {INMUEBLES_URL}/api/inmuebles/clave/{propiedadId}` |
| `terrenos-service` | 8084 | `GET {TERRENOS_URL}/api/terrenos/clave/{propiedadId}` |

Primero busca la clave en inmuebles y, si no está, en terrenos. El `propiedadId` de la conversación es la misma clave que usa el catálogo (`casa65`, `chapala`, `lago_nogal`, etc.). Con esos datos (precio, ubicación, m², distribución o medidas, uso de suelo, amenidades y estatus) el asistente responde sin inventar información.

- Desde IntelliJ se usan `http://localhost:8083` y `http://localhost:8084`.
- Desde Docker se usa `host.docker.internal`, que apunta a los catálogos levantados en tu equipo.
- Se cambian con las variables `INMUEBLES_URL` y `TERRENOS_URL`.

Si los catálogos están apagados o no tienen la propiedad, el chat sigue funcionando y el asistente indica que un asesor puede dar la información.

## Pruebas

Las pruebas usan una base H2 en memoria (perfil `test`) y simulan Gemini, así que no necesitan Docker ni la clave de la API:

```bash
./mvnw test
```

## Postman

La colección `postman/chat-ia-service.postman_collection.json` trae todas las peticiones. Impórtala en Postman y ejecútalas en orden: "Crear conversación" guarda el id en la variable `conversacionId` que usan las demás.

## Base de datos

Docker crea una base MariaDB llamada:

```text
chat_ia_db
```

Las tablas principales son:

- `conversaciones`
- `mensajes`

Los datos se conservan en un volumen de Docker aunque los contenedores sean detenidos.

## Seguridad

- La clave de Gemini no se guarda en el código.
- Las contraseñas se proporcionan mediante variables de entorno.
- El archivo `.env` está excluido mediante `.gitignore`.
- La autenticación con JWT será gestionada posteriormente por el microservicio correspondiente.

## Mejoras futuras

- Recepción y validación de JWT.
- Registro en Eureka.
- Comunicación por medio de Kafka si el proyecto lo requiere.
- Integración con la interfaz web.