# Cómo integrarse con el microservicio `autenticacion`

Guía para los microservicios del equipo que necesitan saber **quién hace la petición** y **qué puede hacer**:
el **chat con IA** (solo PREMIUM) y el **microservicio de pago** (activa PREMIUM).
Autora del microservicio: rama `liz`.

---

## 1. Conceptos rápidos

| Quién | Cómo se reconoce | En una función privada |
|---|---|---|
| **INVITADO** | La petición llega **sin token** (o con uno inválido o vencido) | **401** |
| **USUARIO ESTANDAR** | Token válido, `accesoPremium: false` | **403** si la función es PREMIUM |
| **USUARIO PREMIUM** | Token válido, `plan: "PREMIUM"`, `accesoPremium: true` | ✅ Permitido |
| **ADMIN** | Token válido, `rol: "ADMIN"` (siempre tiene `accesoPremium: true`) | ✅ Permitido |

- **401** = "no sé quién eres" → que inicie sesión.
- **403** = "sé quién eres, pero no tienes permiso" → por ejemplo, que contrate PREMIUM.

El token lo obtiene la página web con `POST /api/auth/login` y lo envía en **cada petición** con el encabezado:

```
Authorization: Bearer <token>
```

Tu microservicio **no necesita librerías de JWT ni la clave secreta**: solo reenvía ese encabezado a `autenticacion`.

---

## 2. Dirección de `autenticacion`

Ponla en el `application.properties` de tu microservicio, para cambiarla en un solo lugar:

| Dónde corre | URL |
|---|---|
| En tu PC (IntelliJ) | `http://localhost:8082` |
| En Docker (red `grupo1015`) | `http://autenticacion:8082` |
| Con Eureka (más adelante) | `http://autenticacion` |

```properties
servicios.autenticacion.url=http://localhost:8082
```

---

## 3. `GET /api/auth/validar` (lo que usa el chat con IA)

**Petición**

```
GET /api/auth/validar
Authorization: Bearer <token que te envió la página>
```

**Respuesta 200** (token válido)

```json
{
  "cuentaId": 2,
  "correo": "maria@correo.com",
  "usuarioId": 2,
  "rol": "USUARIO",
  "plan": "PREMIUM",
  "accesoPremium": true
}
```

| Campo | Significado |
|---|---|
| `cuentaId` | Id de la cuenta en `autenticacion` (es el que usa el pago para activar PREMIUM) |
| `usuarioId` | Id del perfil en el microservicio `usuario` (nombre, apellido) |
| `rol` | `ADMIN` o `USUARIO` |
| `plan` | `ESTANDAR` o `PREMIUM` |
| `accesoPremium` | **Revisa este campo para el chat**: `true` si es PREMIUM o ADMIN |

**Respuesta 401**: no hay token, es falso, fue modificado o ya venció.

```json
{ "error": "Token invalido o vencido, inicia sesion de nuevo" }
```

> El `rol` y el `plan` se leen de la base de datos **en el momento de la consulta**. Si alguien pasa a PREMIUM,
> `/validar` lo refleja de inmediato, sin que la persona tenga que volver a iniciar sesión.

---

## 4. Ejemplo para el chat con IA (Spring Boot + Spring MVC)

No necesitas agregar dependencias: `RestClient` ya viene en Spring MVC.

**`client/AutenticacionClient.java`**

```java
@Component
public class AutenticacionClient {

    private final RestClient restClient;

    public AutenticacionClient(@Value("${servicios.autenticacion.url}") String url) {
        this.restClient = RestClient.builder().baseUrl(url).build();
    }

    // Devuelve los datos de la cuenta, o null si es un invitado / token invalido (401).
    // Si autenticacion esta apagado lanza ResourceAccessException.
    public Cuenta validar(String authorization) {
        if (authorization == null) {
            return null; // invitado
        }
        try {
            return restClient.get()
                    .uri("/api/auth/validar")
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(Cuenta.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            return null;
        }
    }

    public record Cuenta(Long cuentaId, String correo, Long usuarioId,
                         String rol, String plan, boolean accesoPremium) {
    }
}
```

**En tu controlador del chat**

```java
@PostMapping("/api/chat")
public ResponseEntity<?> chatear(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody MensajeRequest mensaje) {

    AutenticacionClient.Cuenta cuenta;
    try {
        cuenta = autenticacionClient.validar(authorization);
    } catch (ResourceAccessException e) {
        return ResponseEntity.status(503).body(Map.of("error", "Autenticacion no disponible"));
    }

    if (cuenta == null) {
        return ResponseEntity.status(401).body(Map.of("error", "Inicia sesion para usar el chat"));
    }
    if (!cuenta.accesoPremium()) {
        return ResponseEntity.status(403).body(Map.of("error", "El chat con IA requiere plan PREMIUM"));
    }

    // ... aqui va la logica del chat (cuenta.usuarioId() identifica a la persona)
}
```

---

## 5. Microservicio de pago: activar PREMIUM

Flujo propuesto:

1. La persona paga. La página envía su token al microservicio de pago.
2. Pago llama a `GET /api/auth/validar` con ese token y obtiene su **`cuentaId`**.
3. Cuando el pago se confirma, pago llama al endpoint de administrador:

```
PUT /api/auth/admin/cuentas/{cuentaId}/plan
Authorization: Bearer <token de ADMIN>
Content-Type: application/json

{ "plan": "PREMIUM" }
```

Respuesta **200**:

```json
{ "id": 2, "correo": "maria@correo.com", "usuarioId": 2, "rol": "USUARIO", "plan": "PREMIUM" }
```

**¿De dónde saca pago el token de ADMIN?** (propuesta provisional)

- Se crea una cuenta solo para el servicio de pago (por ejemplo `pago@grupo1015.com`) y un ADMIN le da rol ADMIN
  con `PUT /api/auth/admin/cuentas/{id}/rol` y el cuerpo `{"rol": "ADMIN"}`.
- Pago guarda ese correo y esa contraseña en **su propio `.env`** (nunca en el código ni en GitHub).
- Pago hace `POST /api/auth/login` para obtener el token. El token dura **60 minutos**: si recibe un **401**, vuelve
  a iniciar sesión y reintenta.

Para cancelar la suscripción: el mismo endpoint con `{ "plan": "ESTANDAR" }`.

---

## 6. Otros endpoints de `autenticacion`

| Método y ruta | Quién | Cuerpo | Respuestas |
|---|---|---|---|
| `POST /api/auth/registro` | Cualquiera | `{nombre, apellido, correo, password}` | 201, 400, 409, 503 |
| `POST /api/auth/login` | Cualquiera | `{correo, password}` | 200 `{token, tipo, expiraEnMinutos}`, 401 |
| `GET /api/auth/validar` | Con token | — | 200, 401 |
| `GET /api/auth/admin/cuentas` | ADMIN | — | 200, 401, 403 |
| `PUT /api/auth/admin/cuentas/{id}/plan` | ADMIN | `{plan}` | 200, 400, 401, 403, 404 |
| `PUT /api/auth/admin/cuentas/{id}/rol` | ADMIN | `{rol}` | 200, 400, 401, 403, 404, 409 |

Todos los errores tienen la forma `{"error": "mensaje"}`.
La colección de Postman con todos los casos está en `postman/autenticacion.postman_collection.json`.

---

## 7. Pendientes conocidos

- **CORS**: si la página web llama a `autenticacion` directamente desde el navegador, hay que permitir su origen
  (o pasar por el API Gateway). Avisar a la autora cuando se conecte la página.
- **Eureka**: cuando se agregue, las URLs cambian a `http://autenticacion` (sección 2).
