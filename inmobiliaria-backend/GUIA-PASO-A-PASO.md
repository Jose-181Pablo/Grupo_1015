# Guía paso a paso: microservicio de Inmobiliaria

**Grupo 10.15 · Spring Boot 4.0.8 · Java 25 · PostgreSQL · Eureka · Docker · Postman · IntelliJ · GitHub**

Esta guía convierte la sección de casas y terrenos de `inmobiliaria.html` en un microservicio con patrón MVC. Todo el código ya fue compilado con Java 25 y probado contra PostgreSQL real: las 13 peticiones de la colección de Postman pasan y el servicio aparece registrado en Eureka.

Los 2 microservicios que vas a tener son:

| Microservicio | Puerto | Para qué sirve |
|---|---|---|
| `eureka-server` | 8761 | El "directorio telefónico". Los 8 microservicios del equipo se registran aquí para encontrarse por nombre. Solo se necesita **uno** para todo el equipo. |
| `inmobiliaria-service` | 8082 | Tu servicio: el catálogo de casas y terrenos (crear, consultar, editar, borrar). |

```
             ┌──────────────────────┐
             │  eureka-server :8761 │  ← todos se registran aquí
             └──────────▲───────────┘
                        │ se registra como INMOBILIARIA-SERVICE
Postman / página web ──►│
   GET /api/propiedades ┌┴─────────────────────────────┐      ┌────────────────────┐
                        │ inmobiliaria-service :8082   │─────►│ PostgreSQL :5432   │
                        │ Controller → Service → Repo  │      │ inmobiliaria_db    │
                        └──────────────────────────────┘      └────────────────────┘
```

---

## Paso 1. Instala lo necesario (solo la primera vez)

1. **IntelliJ IDEA** (Community sirve; Ultimate trae extras para bases de datos).
2. **Java 25**: en IntelliJ ve a `File → Project Structure → Project → SDK → Add SDK → Download JDK…`, elige versión **25** y proveedor **Eclipse Temurin**. No necesitas instalar Java aparte.
3. **Docker Desktop**: instálalo y ábrelo. Debe quedar la ballena en verde ("Engine running").
4. **Postman**: la app de escritorio.
5. **Git**: ya lo tienes, porque clonaste el repositorio.

> No necesitas instalar PostgreSQL en tu computadora. Docker lo levanta por ti.

---

## Paso 2. Copia el código a tu repositorio clonado

1. Descarga el archivo **`inmobiliaria-backend.zip`** que te dejé en este hilo.
2. Descomprímelo **dentro de la carpeta de tu repositorio clonado**. Debe quedar así:

```
tu-repositorio/
├── (lo que ya tenía el repo: index.html, inmobiliaria.html, ...)
└── inmobiliaria-backend/
    ├── .gitignore
    ├── docker-compose.yml
    ├── eureka-server/
    │   ├── Dockerfile
    │   ├── pom.xml
    │   └── src/main/...
    ├── inmobiliaria-service/
    │   ├── Dockerfile
    │   ├── pom.xml
    │   └── src/main/
    │       ├── java/com/grupo1015/inmobiliaria/
    │       │   ├── InmobiliariaServiceApplication.java
    │       │   ├── model/        Propiedad.java, Categoria.java, Estatus.java
    │       │   ├── repository/   PropiedadRepository.java
    │       │   ├── service/      PropiedadService.java
    │       │   ├── controller/   PropiedadController.java
    │       │   └── exception/    GlobalExceptionHandler.java y 2 excepciones
    │       └── resources/        application.yml, data.sql
    └── postman/
        └── inmobiliaria-service.postman_collection.json
```

> Si tu equipo ya acordó otra estructura en el repo (por ejemplo, una carpeta por microservicio en la raíz), mueve `eureka-server/` e `inmobiliaria-service/` ahí. El código no cambia.

---

## Paso 3. Abre el proyecto en IntelliJ

1. `File → Open…` y elige la carpeta de **tu repositorio** (la raíz, no la de un microservicio).
2. Abre la pestaña **Maven** (lado derecho). Pulsa el **+** ("Add Maven Projects") y agrega estos dos archivos:
   - `inmobiliaria-backend/eureka-server/pom.xml`
   - `inmobiliaria-backend/inmobiliaria-service/pom.xml`
3. Espera a que termine de descargar dependencias (barra de progreso abajo). La primera vez tarda unos minutos.
4. Verifica el SDK: `File → Project Structure → Project → SDK = 25`.

✅ **Cómo sabes que vas bien:** no hay archivos `.java` en rojo, y en el panel Maven aparecen `eureka-server` e `inmobiliaria-service`.

---

## Paso 4. Entiende el MVC antes de correrlo

Cada microservicio del equipo debe seguir esta misma estructura:

| Capa | Archivo | Qué hace |
|---|---|---|
| **Modelo** | `model/Propiedad.java` | Define qué es una propiedad. JPA lo convierte en la tabla `propiedades`. Los campos salen de tu página: ubicación, inversión (precio), terreno m², construcción m², distribución, amenidades, imagen y ficha técnica PDF. |
| | `model/Categoria.java` | `CASA` o `TERRENO`, las dos secciones de `inmobiliaria.html`. |
| | `model/Estatus.java` | `DISPONIBLE`, `APARTADA` o `VENDIDA`. |
| **Repositorio** | `repository/PropiedadRepository.java` | Habla con PostgreSQL. Spring genera el SQL a partir del nombre del método (`findByCategoria`…). |
| **Servicio** | `service/PropiedadService.java` | Reglas de negocio: no repetir claves, lanzar error si no existe, etc. |
| **Controlador** | `controller/PropiedadController.java` | Recibe las peticiones HTTP y responde JSON. |
| **Vista** | El JSON | En microservicios la vista es el JSON. Postman o la página web lo muestran. |
| Extra | `exception/GlobalExceptionHandler.java` | Convierte errores en respuestas claras (400, 404, 409). |
| Extra | `resources/data.sql` | Carga las **14 propiedades reales** de tu página al arrancar, sin duplicarlas. |

**Endpoints disponibles:**

| Método | URL | Qué hace |
|---|---|---|
| GET | `/api/propiedades` | Lista todas |
| GET | `/api/propiedades?categoria=CASA` | Filtra por `CASA` o `TERRENO` |
| GET | `/api/propiedades?ubicacion=tlajomulco` | Busca por texto en la ubicación |
| GET | `/api/propiedades/{id}` | Una por id |
| GET | `/api/propiedades/clave/{clave}` | Una por clave de la página (`capri`, `lago_nogal`…) |
| POST | `/api/propiedades` | Crea (responde 201) |
| PUT | `/api/propiedades/{id}` | Actualiza |
| DELETE | `/api/propiedades/{id}` | Borra (responde 204) |
| GET | `/actuator/health` | Dice si el servicio está vivo |

---

## Paso 5. Levanta PostgreSQL con Docker

Abre la **Terminal** de IntelliJ (abajo) y escribe:

```bash
cd inmobiliaria-backend
docker compose up -d postgres-inmobiliaria
```

Esto crea un contenedor con PostgreSQL 17, la base `inmobiliaria_db`, usuario `postgres` y contraseña `postgres`, en el puerto **5432**. Los datos se guardan en un volumen, así que no se pierden al apagar.

✅ **Cómo sabes que vas bien:** `docker ps` muestra `postgres-inmobiliaria` con estado `healthy`.

> ⚠️ Si ya tienes PostgreSQL instalado en tu compu usando el puerto 5432, da error de puerto ocupado. Detén ese PostgreSQL, o cambia en `docker-compose.yml` `"5432:5432"` por `"5433:5432"` y en `application.yml` el `5432` de la url por `5433`.

---

## Paso 6. Corre Eureka Server desde IntelliJ

1. Abre `eureka-server/src/main/java/com/grupo1015/eureka/EurekaServerApplication.java`.
2. Da clic en el ▶️ verde junto a `public class` y elige **Run**.
3. Abre en tu navegador **http://localhost:8761**.

✅ Ves el panel de Eureka. En "Instances currently registered" todavía no hay nada.

---

## Paso 7. Corre el microservicio de Inmobiliaria

1. Abre `inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/InmobiliariaServiceApplication.java`.
2. ▶️ **Run**.
3. En la consola verás el SQL de Hibernate creando la tabla y al final `Started InmobiliariaServiceApplication`.

✅ **Comprueba tres cosas:**
- http://localhost:8082/actuator/health responde `{"status":"UP"}`.
- http://localhost:8082/api/propiedades muestra las 14 propiedades de tu página.
- Recarga http://localhost:8761: ahora aparece **INMOBILIARIA-SERVICE**. Puede tardar unos 30 segundos.

---

## Paso 8. Prueba con Postman

1. En Postman: **Import** y arrastra `inmobiliaria-backend/postman/inmobiliaria-service.postman_collection.json`.
2. Aparece la colección **"Inmobiliaria Service - Grupo 10.15"** con 13 peticiones numeradas.
3. Puedes correrlas una por una en orden, o todas juntas: clic derecho en la colección → **Run collection** → **Run**.
   - La **6. Crear propiedad** guarda el `id` nuevo en la variable `{{id}}`. Por eso la 7, 8 y 9 trabajan sobre esa misma propiedad.
   - La **10** y la **11** fallan a propósito, para que veas los mensajes de error 400 y 404.
4. La URL base está en la variable `baseUrl` de la colección (`http://localhost:8082`). Si cambias el puerto, cámbiala ahí.

Ejemplo de cuerpo para crear (POST):

```json
{
  "clave": "casa_postman",
  "nombre": "Casa de Prueba Postman",
  "categoria": "CASA",
  "ubicacion": "Zapopan, Jalisco",
  "precio": 3500000,
  "terrenoM2": 160,
  "construccionM2": 200,
  "distribucion": "3 rec + 2 baños",
  "amenidades": "Alberca + Seguridad 24/7"
}
```

Obligatorios: `clave` (única), `nombre`, `categoria` y `ubicacion`. Todo lo demás es opcional.

---

## Paso 9. Revisa los datos dentro de PostgreSQL

Desde la terminal:

```bash
docker exec -it postgres-inmobiliaria psql -U postgres -d inmobiliaria_db -c "SELECT id, clave, nombre, categoria, precio FROM propiedades;"
```

Si tienes IntelliJ Ultimate, también puedes usar la pestaña **Database** → `+` → PostgreSQL → host `localhost`, puerto `5432`, usuario y contraseña `postgres`, base `inmobiliaria_db`. Con pgAdmin o DBeaver se usan los mismos datos.

---

## Paso 10. Todo dentro de Docker (como en "producción")

Hasta aquí corriste Eureka y tu servicio desde IntelliJ. Ahora los vas a meter también en Docker:

1. **Detén** las dos apps en IntelliJ (cuadro rojo ⏹), para liberar los puertos 8761 y 8082.
2. En la terminal, dentro de `inmobiliaria-backend`:

```bash
docker compose up --build
```

Docker compila cada microservicio con Maven y Java 25 (usa su `Dockerfile`) y levanta los 3 contenedores: Postgres, Eureka e Inmobiliaria. La primera vez tarda varios minutos.

3. Repite las pruebas: http://localhost:8761 y la colección de Postman deben funcionar igual.

Para apagar: `Ctrl + C` y luego `docker compose down`. Para borrar también la base de datos: `docker compose down -v`.

> ¿Por qué funciona igual? `application.yml` usa `localhost` por defecto, pero `docker-compose.yml` manda las variables `DB_URL` y `EUREKA_URL` con los nombres de los contenedores. Dentro de Docker, los contenedores se encuentran por su nombre, no por `localhost`.

---

## Paso 11. Sube tu trabajo a GitHub

Trabaja siempre en una **rama** propia, no directo en `main`:

```bash
# desde la raíz de tu repositorio
git checkout -b feature/inmobiliaria-service
git add inmobiliaria-backend
git status          # revisa que NO aparezcan carpetas target/ ni .idea/
git commit -m "Agrega microservicio de inmobiliaria con Eureka, PostgreSQL y Docker"
git push -u origin feature/inmobiliaria-service
```

Después, en GitHub, da clic en **"Compare & pull request"** para que el equipo lo revise y lo una a `main`.

Desde IntelliJ también se puede: `Git → New Branch…`, luego `Ctrl + K` (Commit), escribes el mensaje y **Commit and Push**.

Cada vez que hagas cambios: `git add .` → `git commit -m "qué cambiaste"` → `git push`.

> El `.gitignore` incluido evita subir `target/` (compilados), `.idea/` (configuración personal de IntelliJ) y archivos `.env`.

---

## Paso 12. Cómo se conecta con los otros microservicios del equipo

- **Un solo Eureka para los 8.** Que el equipo use este `eureka-server` (o uno igual) en el puerto 8761. Cada compañero agrega a su servicio la dependencia `spring-cloud-starter-netflix-eureka-client` y en su `application.yml` pone:

  ```yaml
  spring:
    application:
      name: nombre-de-su-servicio
  eureka:
    client:
      service-url:
        defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
  ```

- **Mismas versiones en todos.** Spring Boot **4.0.8**, Spring Cloud **2025.1.3** y Java **25**. Si alguien genera su proyecto en start.spring.io, que elija esas.
- **Un puerto distinto por servicio.** Inmobiliaria usa 8082; los demás deben usar otro (8081, 8083, 8084…).
- **Una base de datos por microservicio.** Cada quien tiene la suya; nadie lee tablas de otro. Si otro servicio necesita una propiedad, la pide por HTTP a `http://inmobiliaria-service/api/propiedades/{id}` usando el nombre registrado en Eureka.
- **Un solo `docker-compose.yml` al final.** Cuando estén todos, se juntan sus bloques de `services:` en un archivo general.

---

## Paso 13. Preparación para JWT (autenticación)

Todavía **no** se implementa, porque eso le toca al microservicio de autenticación. Lo que ya quedó listo:

- La propiedad tiene el campo **`usuarioId`** (opcional por ahora). Ahí se guardará qué asesor la dio de alta.

Cuando el servicio de autenticación ya entregue tokens JWT, los cambios en inmobiliaria serán estos:

1. Agregar al `pom.xml` la dependencia `spring-boot-starter-oauth2-resource-server`.
2. Configurar en `application.yml` la clave o URL con la que el servicio de autenticación firma los tokens.
3. Dejar los `GET` públicos (el catálogo lo ve cualquiera) y exigir token en `POST`, `PUT` y `DELETE`.
4. Tomar el `usuarioId` desde el token en lugar de recibirlo en el JSON.
5. En Postman, mandar en esas peticiones el header `Authorization: Bearer <token>`.

---

## Paso 14 (opcional). Usar la API desde la página estática

Tu `inmobiliaria.html` hoy tiene las propiedades escritas a mano. Más adelante puede pedirlas al microservicio así:

```javascript
fetch('http://localhost:8082/api/propiedades?categoria=CASA')
  .then(res => res.json())
  .then(propiedades => {
    propiedades.forEach(p => console.log(p.nombre, p.precio, p.imagenUrl));
  });
```

El controlador ya tiene `@CrossOrigin`, así que el navegador permite la llamada.

---

## Errores comunes

| Síntoma | Causa y solución |
|---|---|
| `Connection refused` a `localhost:5432` | PostgreSQL no está arriba. Corre `docker compose up -d postgres-inmobiliaria` (Paso 5). |
| `password authentication failed` | Usuario o contraseña no coinciden entre `docker-compose.yml` y `application.yml`. Deben ser `postgres` / `postgres`. |
| `Port 8082 was already in use` | Ya está corriendo otra copia (IntelliJ o Docker). Detén una. |
| Error de `release version 25` o `invalid target release` | El SDK no es Java 25. Revisa el Paso 1 y `Project Structure`. |
| No aparece en Eureka | Eureka tiene que estar arriba **antes**. Espera unos 30 segundos y recarga la página. |
| Postman responde 400 "Formato incorrecto" | `categoria` debe ir exactamente `CASA` o `TERRENO`, en mayúsculas. |
| Postman responde 409 | Ya existe otra propiedad con esa `clave`. Usa otra. |
| `.java` en rojo en IntelliJ | Falta recargar Maven: panel Maven → 🔄 "Reload All Maven Projects". |

---

# Anexo: código completo de cada archivo

Es el mismo código del zip, por si prefieres crear los archivos a mano en IntelliJ (`clic derecho → New → Java Class / File`).

### `inmobiliaria-backend/eureka-server/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.8</version>
        <relativePath/>
    </parent>

    <groupId>com.grupo1015</groupId>
    <artifactId>eureka-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>eureka-server</name>
    <description>Servidor de descubrimiento compartido por todos los microservicios</description>

    <properties>
        <java.version>25</java.version>
        <spring-cloud.version>2025.1.3</spring-cloud.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### `inmobiliaria-backend/eureka-server/src/main/java/com/grupo1015/eureka/EurekaServerApplication.java`

```java
package com.grupo1015.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

### `inmobiliaria-backend/eureka-server/src/main/resources/application.yml`

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    # El servidor no se registra a sí mismo ni busca otros servidores
    register-with-eureka: false
    fetch-registry: false
```

### `inmobiliaria-backend/eureka-server/Dockerfile`

```dockerfile
# Etapa 1: compilar el .jar con Maven y Java 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Etapa 2: imagen ligera solo con el JRE para ejecutar
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `inmobiliaria-backend/inmobiliaria-service/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.8</version>
        <relativePath/>
    </parent>

    <groupId>com.grupo1015</groupId>
    <artifactId>inmobiliaria-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>inmobiliaria-service</name>
    <description>Microservicio de propiedades (casas y terrenos) del Grupo 10.15</description>

    <properties>
        <java.version>25</java.version>
        <spring-cloud.version>2025.1.3</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- API REST (la "C" de MVC: controladores) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <!-- JPA / Hibernate para hablar con PostgreSQL (la "M" de MVC: modelo) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <!-- Validaciones: @NotBlank, @Positive, etc. -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <!-- /actuator/health para saber si el servicio está vivo -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <!-- Registro en Eureka -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <!-- Driver de PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/InmobiliariaServiceApplication.java`

```java
package com.grupo1015.inmobiliaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InmobiliariaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InmobiliariaServiceApplication.class, args);
    }
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/model/Propiedad.java`

```java
package com.grupo1015.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MODELO: una casa o terreno del catálogo.
 * Los campos salen de la lista "project-meta" de cada propiedad en inmobiliaria.html
 * (Ubicación, Inversión, Terreno, Construcción, Distribución, Amenidades, Ficha técnica...).
 */
@Entity
@Table(name = "propiedades")
public class Propiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador corto que ya usa la página, p. ej. "casa65" o "lago_nogal". */
    @NotBlank(message = "La clave es obligatoria")
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotNull(message = "La categoría es obligatoria (CASA o TERRENO)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String ubicacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Precio en MXN. Puede ir vacío si la página no lo muestra. */
    @Positive(message = "El precio debe ser mayor a 0")
    @Column(precision = 14, scale = 2)
    private BigDecimal precio;

    @Positive(message = "Los m² de terreno deben ser mayores a 0")
    @Column(name = "terreno_m2", precision = 10, scale = 2)
    private BigDecimal terrenoM2;

    @Positive(message = "Los m² de construcción deben ser mayores a 0")
    @Column(name = "construccion_m2", precision = 10, scale = 2)
    private BigDecimal construccionM2;

    @Size(max = 255)
    private String distribucion;

    @Size(max = 500)
    @Column(length = 500)
    private String amenidades;

    @Size(max = 255)
    @Column(name = "imagen_url")
    private String imagenUrl;

    @Size(max = 255)
    @Column(name = "ficha_tecnica_url")
    private String fichaTecnicaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estatus estatus = Estatus.DISPONIBLE;

    /**
     * Id del usuario (asesor) que dio de alta la propiedad.
     * Hoy es opcional; cuando exista el microservicio de autenticación con JWT,
     * se llenará automáticamente con el id que venga dentro del token.
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
        if (estatus == null) {
            estatus = Estatus.DISPONIBLE;
        }
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    // ----- Getters y setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public BigDecimal getTerrenoM2() { return terrenoM2; }
    public void setTerrenoM2(BigDecimal terrenoM2) { this.terrenoM2 = terrenoM2; }

    public BigDecimal getConstruccionM2() { return construccionM2; }
    public void setConstruccionM2(BigDecimal construccionM2) { this.construccionM2 = construccionM2; }

    public String getDistribucion() { return distribucion; }
    public void setDistribucion(String distribucion) { this.distribucion = distribucion; }

    public String getAmenidades() { return amenidades; }
    public void setAmenidades(String amenidades) { this.amenidades = amenidades; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getFichaTecnicaUrl() { return fichaTecnicaUrl; }
    public void setFichaTecnicaUrl(String fichaTecnicaUrl) { this.fichaTecnicaUrl = fichaTecnicaUrl; }

    public Estatus getEstatus() { return estatus; }
    public void setEstatus(Estatus estatus) { this.estatus = estatus; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/model/Categoria.java`

```java
package com.grupo1015.inmobiliaria.model;

/** Las dos secciones de inmobiliaria.html: "Residencial / Casas" e "Inversión / Terrenos". */
public enum Categoria {
    CASA,
    TERRENO
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/model/Estatus.java`

```java
package com.grupo1015.inmobiliaria.model;

public enum Estatus {
    DISPONIBLE,
    APARTADA,
    VENDIDA
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/repository/PropiedadRepository.java`

```java
package com.grupo1015.inmobiliaria.repository;

import com.grupo1015.inmobiliaria.model.Categoria;
import com.grupo1015.inmobiliaria.model.Propiedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORIO: Spring genera el SQL a partir del nombre de cada método.
 * JpaRepository ya trae findAll, findById, save, deleteById, etc.
 */
public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {

    Optional<Propiedad> findByClave(String clave);

    boolean existsByClave(String clave);

    List<Propiedad> findByCategoria(Categoria categoria);

    List<Propiedad> findByUbicacionContainingIgnoreCase(String texto);

    List<Propiedad> findByCategoriaAndUbicacionContainingIgnoreCase(Categoria categoria, String texto);
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/service/PropiedadService.java`

```java
package com.grupo1015.inmobiliaria.service;

import com.grupo1015.inmobiliaria.exception.RecursoDuplicadoException;
import com.grupo1015.inmobiliaria.exception.RecursoNoEncontradoException;
import com.grupo1015.inmobiliaria.model.Categoria;
import com.grupo1015.inmobiliaria.model.Propiedad;
import com.grupo1015.inmobiliaria.repository.PropiedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SERVICIO: aquí van las reglas de negocio.
 * El controlador nunca habla directo con el repositorio, siempre pasa por aquí.
 */
@Service
public class PropiedadService {

    private final PropiedadRepository repository;

    public PropiedadService(PropiedadRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Propiedad> listar(Categoria categoria, String ubicacion) {
        boolean hayUbicacion = ubicacion != null && !ubicacion.isBlank();
        if (categoria != null && hayUbicacion) {
            return repository.findByCategoriaAndUbicacionContainingIgnoreCase(categoria, ubicacion);
        }
        if (categoria != null) {
            return repository.findByCategoria(categoria);
        }
        if (hayUbicacion) {
            return repository.findByUbicacionContainingIgnoreCase(ubicacion);
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Propiedad buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la propiedad con id " + id));
    }

    @Transactional(readOnly = true)
    public Propiedad buscarPorClave(String clave) {
        return repository.findByClave(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la propiedad con clave " + clave));
    }

    @Transactional
    public Propiedad crear(Propiedad propiedad) {
        if (repository.existsByClave(propiedad.getClave())) {
            throw new RecursoDuplicadoException("Ya existe una propiedad con clave " + propiedad.getClave());
        }
        propiedad.setId(null); // el id lo genera PostgreSQL
        return repository.save(propiedad);
    }

    @Transactional
    public Propiedad actualizar(Long id, Propiedad datos) {
        Propiedad actual = buscarPorId(id);

        if (!actual.getClave().equals(datos.getClave()) && repository.existsByClave(datos.getClave())) {
            throw new RecursoDuplicadoException("Ya existe una propiedad con clave " + datos.getClave());
        }

        actual.setClave(datos.getClave());
        actual.setNombre(datos.getNombre());
        actual.setCategoria(datos.getCategoria());
        actual.setUbicacion(datos.getUbicacion());
        actual.setDescripcion(datos.getDescripcion());
        actual.setPrecio(datos.getPrecio());
        actual.setTerrenoM2(datos.getTerrenoM2());
        actual.setConstruccionM2(datos.getConstruccionM2());
        actual.setDistribucion(datos.getDistribucion());
        actual.setAmenidades(datos.getAmenidades());
        actual.setImagenUrl(datos.getImagenUrl());
        actual.setFichaTecnicaUrl(datos.getFichaTecnicaUrl());
        if (datos.getEstatus() != null) {
            actual.setEstatus(datos.getEstatus());
        }
        actual.setUsuarioId(datos.getUsuarioId());

        return repository.save(actual);
    }

    @Transactional
    public void eliminar(Long id) {
        Propiedad propiedad = buscarPorId(id);
        repository.delete(propiedad);
    }
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/controller/PropiedadController.java`

```java
package com.grupo1015.inmobiliaria.controller;

import com.grupo1015.inmobiliaria.model.Categoria;
import com.grupo1015.inmobiliaria.model.Propiedad;
import com.grupo1015.inmobiliaria.service.PropiedadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CONTROLADOR: recibe las peticiones HTTP (Postman o la página web) y responde JSON.
 * En un microservicio la "vista" es ese JSON; la página estática lo pintará con fetch().
 */
@RestController
@RequestMapping("/api/propiedades")
@CrossOrigin(origins = "*") // permite que la página estática llame a la API desde el navegador
public class PropiedadController {

    private final PropiedadService service;

    public PropiedadController(PropiedadService service) {
        this.service = service;
    }

    // GET /api/propiedades            -> todas
    // GET /api/propiedades?categoria=CASA&ubicacion=tlajomulco -> filtradas
    @GetMapping
    public List<Propiedad> listar(@RequestParam(required = false) Categoria categoria,
                                  @RequestParam(required = false) String ubicacion) {
        return service.listar(categoria, ubicacion);
    }

    @GetMapping("/{id}")
    public Propiedad buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/clave/{clave}")
    public Propiedad buscarPorClave(@PathVariable String clave) {
        return service.buscarPorClave(clave);
    }

    @PostMapping
    public ResponseEntity<Propiedad> crear(@Valid @RequestBody Propiedad propiedad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(propiedad));
    }

    @PutMapping("/{id}")
    public Propiedad actualizar(@PathVariable Long id, @Valid @RequestBody Propiedad propiedad) {
        return service.actualizar(id, propiedad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/exception/RecursoNoEncontradoException.java`

```java
package com.grupo1015.inmobiliaria.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/exception/RecursoDuplicadoException.java`

```java
package com.grupo1015.inmobiliaria.exception;

public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/java/com/grupo1015/inmobiliaria/exception/GlobalExceptionHandler.java`

```java
package com.grupo1015.inmobiliaria.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte los errores en respuestas JSON claras, en lugar de un error 500 genérico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> duplicado(RecursoDuplicadoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> formato(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Formato incorrecto. Revisa el JSON y que categoria sea CASA o TERRENO", null);
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje, Object detalles) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("fecha", LocalDateTime.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", mensaje);
        if (detalles != null) {
            cuerpo.put("detalles", detalles);
        }
        return ResponseEntity.status(status).body(cuerpo);
    }
}
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    # Con este nombre aparece en Eureka y lo encontrarán los otros microservicios
    name: inmobiliaria-service

  datasource:
    # ${VARIABLE:valor} = usa la variable de entorno si existe; si no, el valor por defecto.
    # Desde IntelliJ se usan los valores por defecto (localhost).
    # Desde docker-compose se mandan DB_URL, DB_USER, DB_PASSWORD.
    url: ${DB_URL:jdbc:postgresql://localhost:5432/inmobiliaria_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}

  jpa:
    hibernate:
      ddl-auto: update        # crea/actualiza la tabla "propiedades" automáticamente
    show-sql: true
    open-in-view: false
    defer-datasource-initialization: true   # primero crea la tabla, luego corre data.sql

  sql:
    init:
      mode: always            # carga las propiedades de ejemplo de data.sql

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### `inmobiliaria-backend/inmobiliaria-service/src/main/resources/data.sql`

```sql
-- Propiedades tomadas de inmobiliaria.html (sitio estático del Grupo 10.15).
-- "ON CONFLICT (clave) DO NOTHING" evita duplicarlas cada vez que arranca el servicio.

INSERT INTO propiedades (clave, nombre, categoria, ubicacion, precio, terreno_m2, construccion_m2, distribucion, amenidades, imagen_url, ficha_tecnica_url, estatus, fecha_creacion, fecha_actualizacion) VALUES
('casa65', 'Casa 65', 'CASA', 'Valle Imperial', NULL, NULL, 280, NULL, NULL, 'assets/images/casa65/fachada_principal_.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('almendrado', 'Coto Almendrado', 'CASA', 'Valle Imperial', NULL, NULL, 310, NULL, NULL, 'assets/images/coto_almendrado/fachada_principal.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('ciudadela', 'La Ciudadela 45', 'CASA', 'Casa Fuerte, Coto La Ciudadela', 6300000, 273, 247, '3 rec + Baño/Vestidor + Estudio', NULL, 'assets/images/la_ciudadela_45/FAHCADA PRINCIPAL.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('baluarte', 'Valuarte 67', 'CASA', 'Casa Fuerte, Coto Baluarte', 5200000, 140, 225, '3 rec + Estudio/4ta rec + Roof Garden', NULL, 'assets/images/CASA FUERTE-BALUARTE/FACHADA.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('hacienda', 'Hacienda Agave', 'CASA', 'Hacienda Agave, Tlajomulco', 7800000, 470, 430, NULL, 'Cochera subterránea 4-6 autos', 'assets/images/HACENDA HAGAVE-CASA DE 7.8MDP/FACHADA.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('tlajomulco', 'Hacienda Tlajomulco', 'CASA', 'Centro de Tlajomulco', 2770000, NULL, NULL, '3 rec con baño + Área de TV', 'Coto Privado + Seguridad', 'assets/images/HACIENDA AGAVE-TLAJOMULCO/FACHADA.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('capri', 'Coto Capri', 'CASA', 'Jardín Real, Coto Capri', 11990000, 180, 300, '3 rec con baño + Estudio en PB', NULL, 'assets/images/JARDIN REAL-COTO CAPRI/FACHADA.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('tajo', 'San José del Tajo', 'CASA', 'San José del Tajo (Frente Casa Club)', 11950000, 376, NULL, '4 rec + 4.5 baños + Cuarto servicio', 'Frente a Club Privado', 'assets/images/SAN JOSE DE TAJO/FACHADA.jpg', NULL, 'DISPONIBLE', NOW(), NOW()),
('sendero_monte_verde', 'Sendero Monte Verde', 'CASA', 'Sendero Monte Verde, Tlajomulco', NULL, NULL, NULL, '3 recámaras + Baño + Jardín', 'Jardín con árboles + Patio interior', 'assets/images/SENDERO_MONTE_VERDE/foto_6.jpg', 'assets/images/SENDERO_MONTE_VERDE/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW()),
('chapala', 'Chapala', 'CASA', 'Ribera de Chapala, Jalisco', NULL, NULL, NULL, 'Múltiples recámaras con baño + Área Social', 'Alberca + Terraza + Cocina Integral', 'assets/images/CHAPALA/FACHADA_PRINCIPAL.jpg', 'assets/images/CHAPALA/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW()),
('bosques_santa_anita_2', 'Bosques de Santa Anita', 'CASA', 'Bosques de Santa Anita, Tlajomulco', NULL, NULL, NULL, NULL, 'Acabados de lujo + Clósets, Seguridad 24/7 y áreas verdes', 'assets/images/BOSQUES_SANTA_ANITA_2/foto_1.jpg', 'assets/images/BOSQUES_SANTA_ANITA_2/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW()),
('galerias_santa_anita', 'Galerías Santa Anita', 'TERRENO', 'Sobre Av. López Mateos, Santa Anita (Frente a Galerías)', NULL, 5615.42, NULL, NULL, 'Vocación: Desarrollo Residencial Vertical', 'assets/images/GALERIAS_SANTA_ANITA/ingreso_terreno.jpg', 'assets/images/GALERIAS_SANTA_ANITA/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW()),
('lago_nogal', 'Lago Nogal', 'TERRENO', 'Fracc. Lago Nogal, Zona Sur de Guadalajara', 5490000, 448, NULL, 'Terreno en esquina 16 x 28 m', 'Alberca, Gym, Vapor, Sauna, Tenis, Jogging, Cine, Terrazas', 'assets/images/LAGO_NOGAL/terreno_dia.jpg', 'assets/images/LAGO_NOGAL/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW()),
('bosques_santa_anita', 'Bosques de Santa Anita (Terreno)', 'TERRENO', 'Coto 9 Valles, Bosques de Santa Anita, Tlajomulco', NULL, 332.96, NULL, NULL, 'Áreas infantiles, capilla, terrazas para eventos', 'assets/images/BOSQUES_SANTA_ANITA/terreno_1.jpg', 'assets/images/BOSQUES_SANTA_ANITA/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW())
ON CONFLICT (clave) DO NOTHING;
```

### `inmobiliaria-backend/inmobiliaria-service/Dockerfile`

```dockerfile
# Etapa 1: compilar el .jar con Maven y Java 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Etapa 2: imagen ligera solo con el JRE para ejecutar
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `inmobiliaria-backend/docker-compose.yml`

```yaml
# Levanta todo con:  docker compose up --build
# Apaga todo con:    docker compose down        (agrega -v para borrar también la base de datos)
services:

  postgres-inmobiliaria:
    image: postgres:17
    container_name: postgres-inmobiliaria
    environment:
      POSTGRES_DB: inmobiliaria_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - datos-inmobiliaria:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d inmobiliaria_db"]
      interval: 5s
      timeout: 5s
      retries: 10

  eureka-server:
    build: ./eureka-server
    container_name: eureka-server
    ports:
      - "8761:8761"

  inmobiliaria-service:
    build: ./inmobiliaria-service
    container_name: inmobiliaria-service
    ports:
      - "8082:8082"
    environment:
      # Dentro de Docker los contenedores se encuentran por su nombre, no por "localhost"
      DB_URL: jdbc:postgresql://postgres-inmobiliaria:5432/inmobiliaria_db
      DB_USER: postgres
      DB_PASSWORD: postgres
      EUREKA_URL: http://eureka-server:8761/eureka/
    depends_on:
      postgres-inmobiliaria:
        condition: service_healthy
      eureka-server:
        condition: service_started

volumes:
  datos-inmobiliaria:
```

### `inmobiliaria-backend/.gitignore`

```gitignore
# Maven
target/

# IntelliJ
.idea/
*.iml
out/

# Sistema
.DS_Store
Thumbs.db

# Variables locales / secretos
.env
*.log
```

La colección de Postman (`postman/inmobiliaria-service.postman_collection.json`) se importa directo como archivo; no hace falta copiarla a mano.
