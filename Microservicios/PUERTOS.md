# Puertos de los microservicios

Lista única acordada para que todos los microservicios puedan levantarse al mismo tiempo en la misma computadora. Cada uno tiene su propio puerto de aplicación y de base de datos.

| Puerto | Microservicio | Rama | Base de datos | Puerto de la base en tu equipo | Estado |
|---:|---|---|---|---:|---|
| 8080 | api-gateway | `aldo` | — | — | Listo |
| 8081 | usuario | `liz` | MariaDB | 3308 | Listo |
| 8082 | autenticacion | `liz` | MariaDB | 3309 | Listo |
| 8083 | inmuebles-service (casas) | `pablo` | PostgreSQL | 5433 | Listo |
| 8084 | terrenos-service | `pablo` | PostgreSQL | 5434 | Listo |
| 8085 | chat-ia-service | `aldo` | MariaDB | 3310 | Listo |
| 8086 | microservicio-citas | `gene1` | MariaDB | 3307 | Listo |
| 8087 | microservicio-ofertas | `gene1` | MariaDB | 3312 | Listo |
| 8088 | suscripcion-service | `aldo` | MariaDB | 3311 | Listo |
| 8761 | eureka-server | `aldo` | — | — | Listo |

## Notas

- Eureka y el API Gateway son infraestructura: ayudan a que los demás servicios se encuentren y se usen desde un solo punto.
- El chat busca cada propiedad primero en inmuebles (8083) y luego en terrenos (8084); se configuran con `INMUEBLES_URL` y `TERRENOS_URL`.
- Cada microservicio vive en su carpeta dentro de `Microservicios/`, con su propio `docker-compose.yml` y su propia base de datos.
- `Microservicios/docker-compose.yml` enciende todo junto: `docker compose up -d --build` desde esta carpeta.
