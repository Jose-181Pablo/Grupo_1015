# Microservicio de Citas — Grupo 10.15

Permite que los clientes agenden visitas a las propiedades de Grupo 10.15 y que el equipo las confirme o cancele.

Forma parte de la arquitectura de microservicios del proyecto Grupo 10.15. Tiene su propia base de datos y se comunica únicamente por HTTP (REST/JSON).

## Tecnologías

- Java 25
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Actuator, DevTools)
- MariaDB
- Lombok
- Maven

## Estructura (MVC + capa de servicio)

```
com.grupo1015.microserviciocitas
├── controller   → CitaController: recibe las peticiones HTTP y responde JSON
├── service      → CitaService: reglas de negocio
├── model        → Cita (entidad) y EstadoCita (PENDIENTE, CONFIRMADA, CANCELADA)
├── repository   → CitaRepository: acceso a la base de datos (Spring Data JPA)
└── exception    → CitaNoEncontradaException y ManejadorErrores (respuestas de error limpias)
```

## Configuración

Toda la configuración se lee de variables de entorno, con valores por defecto para desarrollo local:

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `PORT` | `8081` | Puerto del microservicio |
| `DB_URL` | `jdbc:mariadb://localhost:3406/citas_db` | URL de la base de datos |
| `DB_USER` | `grupo1015` | Usuario de la base de datos |
| `DB_PASSWORD` | *(sin valor, obligatoria)* | Contraseña de la base de datos |

> La contraseña nunca se escribe en el código. Cada integrante la configura en su equipo.

## Base de datos

En MariaDB, ejecutar una sola vez:

```sql
CREATE DATABASE citas_db;
CREATE USER 'grupo1015'@'localhost' IDENTIFIED BY 'tu_contraseña';
GRANT ALL PRIVILEGES ON citas_db.* TO 'grupo1015'@'localhost';
FLUSH PRIVILEGES;
```

La tabla `cita` se crea automáticamente al arrancar el microservicio (`ddl-auto=update`).

## Cómo ejecutarlo

**Desde IntelliJ**

1. Abrir `MicroservicioCitasApplication` → ▶ junto a la clase → *Modify Run Configuration...*
2. En *Environment variables* agregar `DB_PASSWORD=tu_contraseña`.
3. Ejecutar con ▶.

**Desde la terminal (PowerShell)**, dentro de la carpeta del microservicio:

```powershell
$env:DB_PASSWORD="tu_contraseña"
.\mvnw spring-boot:run
```

Arrancó bien cuando en la consola aparece `Started MicroservicioCitasApplication`.

## Endpoints

URL base: `http://localhost:8081/api/citas`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/citas` | Lista todas las citas | 200 |
| GET | `/api/citas?estado=CONFIRMADA` | Filtra por estado | 200 |
| GET | `/api/citas/{id}` | Obtiene una cita | 200 / 404 |
| POST | `/api/citas` | Crea una cita (inicia en `PENDIENTE`) | 201 / 400 |
| PATCH | `/api/citas/{id}/estado?estado=CONFIRMADA` | Cambia el estado | 200 / 404 |
| DELETE | `/api/citas/{id}` | Elimina una cita | 204 / 404 |

### Ejemplo: crear una cita

```json
POST /api/citas
{
  "nombre": "Ana López",
  "email": "ana@mail.com",
  "telefono": "3312345678",
  "propiedad": "chapala",
  "fechaHora": "2026-11-10T11:00:00",
  "mensaje": "Quiero ver la casa"
}
```

Campos obligatorios: `nombre` y `email` (con formato válido).

### Errores

| Código | Cuándo | Ejemplo de respuesta |
|---|---|---|
| 400 | Faltan datos o son inválidos | `{ "nombre": "no debe estar vacío" }` |
| 404 | La cita no existe | `{ "error": "No existe la cita con id 99" }` |

## Monitoreo

`GET http://localhost:8081/actuator/health` → indica si el servicio y su base de datos están funcionando (`"status": "UP"`).

## Pruebas

La colección de Postman está en `/postman/grupo1015.postman_collection.json` (raíz del repositorio). Importarla en Postman y usar **Run collection** para ejecutar todas las pruebas; crea datos de prueba y los elimina al terminar.

## Próximas integraciones

- [ ] Registro en **Eureka** (Eureka Discovery Client)
- [ ] Acceso a través del **API Gateway**
- [ ] **Dockerfile** y Docker Compose
- [ ] Vista (MVC) y conexión con el formulario de `contacto.html`
