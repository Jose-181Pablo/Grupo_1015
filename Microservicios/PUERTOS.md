# Puertos de los microservicios

Lista única acordada para que todos los microservicios puedan levantarse al mismo tiempo en la misma computadora. Cada uno tiene su propio puerto de aplicación y de base de datos.

| Puerto | Microservicio | Rama | Base de datos | Puerto de la base en tu equipo | Estado |
|---:|---|---|---|---:|---|
| 8080 | api-gateway | — | — | — | Reservado |
| 8081 | usuario | `liz` | MariaDB | 3308 | Listo |
| 8082 | autenticacion | `liz` | MariaDB | 3309 | Listo |
| 8083 | catálogo (`inmobiliaria-service`) | `pablo` | PostgreSQL | 5433 | Cambiar de 8082 a 8083 |
| 8084 | terreno | `pablo` | PostgreSQL | 5434 | Reservado (se separará del catálogo) |
| 8085 | chat-ia-service | `aldo` | MariaDB | 3310 | Listo |
| 8086 | citas | `gene1` | MariaDB compartida | 3307 | Cambiar de 8081 a 8086 |
| 8087 | ofertas | `gene1` | MariaDB compartida | 3307 | Cambiar de 8082 a 8087 |
| 8088 | suscripcion-service | — | MariaDB | 3311 | Reservado |
| 8761 | eureka-server | `pablo` | — | — | Listo |

## Notas

- Eureka y el API Gateway son infraestructura: ayudan a que los demás servicios se encuentren y se usen desde un solo punto.
- Cuando el catálogo pase al 8083, el chat debe apuntar a `CATALOGO_URL=http://localhost:8083` (o `http://host.docker.internal:8083` desde Docker).
- En `gene1`, citas y ofertas usan 3406 como base por defecto desde IntelliJ y 3307 desde Docker; conviene dejar uno solo.
