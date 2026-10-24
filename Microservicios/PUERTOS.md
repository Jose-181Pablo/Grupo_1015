# Puertos de los microservicios

Cada microservicio usa su propio puerto de aplicación y de base de datos para que todos puedan levantarse al mismo tiempo en la misma computadora.

| Microservicio | Rama | Puerto de la aplicación | Base de datos | Puerto de la base en tu equipo |
|---|---|---:|---|---:|
| usuario | `liz` | 8081 | MariaDB | 3308 |
| citas | `gene1` | 8081 | MariaDB compartida | 3307 |
| ofertas | `gene1` | 8082 | MariaDB compartida | 3307 |
| inmobiliaria-service | `pablo` | 8082 | PostgreSQL | 5433 |
| eureka-server | `pablo` | 8761 | — | — |
| chat-ia-service | `aldo` | **8085** | MariaDB | **3310** |
| pago-service (reservado) | — | **8086** | — | **3311** |

Los puertos 8081 y 8082 se repiten entre ramas; conviene acordar uno distinto para cada servicio antes de unirlas.
