# Microservicio de Ofertas y Remates — Grupo 10.15

Publica y administra las **ofertas** (descuentos por tiempo limitado) y los **remates** (ventas urgentes o bancarias) de las propiedades de Grupo 10.15.

Forma parte de la arquitectura de microservicios del proyecto Grupo 10.15. Tiene su propia base de datos y se comunica únicamente por HTTP (REST/JSON).

## Tecnologías

- Java 25
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Actuator, DevTools)
- MariaDB
- Lombok
- Maven

## Estructura (MVC + capa de servicio)

```
com.grupo1015.microservicioofertas
├── controller   → OfertaController: recibe las peticiones HTTP y responde JSON
├── service      → OfertaService: reglas de negocio y validaciones
├── model        → Oferta (entidad) y TipoPublicacion (OFERTA, REMATE)
├── repository   → OfertaRepository: acceso a la base de datos (Spring Data JPA)
└── exception    → OfertaNoEncontradaException y ManejadorErrores (respuestas de error limpias)
```

## Reglas de negocio

- El **precio final** no puede ser mayor que el **precio original**.
- La **fecha de fin** no puede ser anterior a la **fecha de inicio**.
- Toda publicación nueva se crea **activa**.
- Desactivar una publicación la oculta de los listados sin borrarla de la base de datos.

## Configuración

Toda la configuración se lee de variables de entorno, con valores por defecto para desarrollo local:

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `PORT` | `8082` | Puerto del microservicio |
| `DB_URL` | `jdbc:mariadb://localhost:3406/ofertas_db` | URL de la base de datos |
| `DB_USER` | `grupo1015` | Usuario de la base de datos |
| `DB_PASSWORD` | *(sin valor, obligatoria)* | Contraseña de la base de datos |

> La contraseña nunca se escribe en el código. Cada integrante la configura en su equipo.

## Base de datos

En MariaDB, ejecutar una sola vez (si el usuario `grupo1015` ya existe, omitir el `CREATE USER`):

```sql
CREATE DATABASE ofertas_db;
CREATE USER 'grupo1015'@'localhost' IDENTIFIED BY 'tu_contraseña';
GRANT ALL PRIVILEGES ON ofertas_db.* TO 'grupo1015'@'localhost';
FLUSH PRIVILEGES;
```

La tabla `oferta` se crea automáticamente al arrancar el microservicio (`ddl-auto=update`).

## Cómo ejecutarlo

**Desde IntelliJ**

1. Abrir `MicroservicioOfertasApplication` → ▶ junto a la clase → *Modify Run Configuration...*
2. En *Environment variables* agregar `DB_PASSWORD=tu_contraseña`.
3. Ejecutar con ▶.

**Desde la terminal (PowerShell)**, dentro de la carpeta del microservicio:

```powershell
$env:DB_PASSWORD="tu_contraseña"
.\mvnw spring-boot:run
```

Arrancó bien cuando en la consola aparece `Started MicroservicioOfertasApplication`.

## Endpoints

URL base: `http://localhost:8082/api/ofertas`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/ofertas` | Lista las publicaciones activas | 200 |
| GET | `/api/ofertas?tipo=REMATE` | Filtra por tipo (`OFERTA` o `REMATE`) | 200 |
| GET | `/api/ofertas?propiedad=chapala` | Publicaciones activas de una propiedad | 200 |
| GET | `/api/ofertas/{id}` | Obtiene una publicación | 200 / 404 |
| POST | `/api/ofertas` | Crea una oferta o remate | 201 / 400 |
| PUT | `/api/ofertas/{id}` | Actualiza una publicación completa | 200 / 400 / 404 |
| PATCH | `/api/ofertas/{id}/desactivar` | Desactiva sin borrar | 200 / 404 |
| DELETE | `/api/ofertas/{id}` | Elimina definitivamente | 204 / 404 |

### Ejemplo: crear una oferta

```json
POST /api/ofertas
{
  "propiedad": "hacienda-agave",
  "titulo": "Casa Hacienda Agave con descuento",
  "descripcion": "Precio especial por tiempo limitado",
  "tipo": "OFERTA",
  "precioOriginal": 7800000,
  "precioFinal": 7350000,
  "fechaInicio": "2026-10-05",
  "fechaFin": "2026-12-31"
}
```

Campos obligatorios: `propiedad`, `titulo`, `tipo`, `precioOriginal` y `precioFinal` (positivos). Las fechas son opcionales.

### Uso desde otros microservicios

Para saber si una propiedad tiene una oferta o remate activo:

```
GET /api/ofertas?propiedad={clave-de-la-propiedad}
```

Devuelve una lista vacía `[]` si no tiene ninguna.

### Errores

| Código | Cuándo | Ejemplo de respuesta |
|---|---|---|
| 400 | Faltan datos o son inválidos | `{ "titulo": "no debe estar vacío" }` |
| 400 | Se rompe una regla de negocio | `{ "error": "El precio final no puede ser mayor que el original" }` |
| 404 | La publicación no existe | `{ "error": "No existe la oferta con id 99" }` |

## Monitoreo

`GET http://localhost:8082/actuator/health` → indica si el servicio y su base de datos están funcionando (`"status": "UP"`).

## Pruebas

La colección de Postman está en `/postman/grupo1015.postman_collection.json` (raíz del repositorio). Importarla en Postman y usar **Run collection** para ejecutar todas las pruebas; crea datos de prueba y los elimina al terminar.

## Próximas integraciones

- [ ] Registro en **Eureka** (Eureka Discovery Client)
- [ ] Acceso a través del **API Gateway**
- [ ] **Dockerfile** y Docker Compose
- [ ] Vista (MVC) para administrar ofertas y remates
