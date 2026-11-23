# Backend de Vuelos Recreativos CAPEBE (Arquitectura de Microservicios)

Proyecto separado en **2 microservicios independientes en Spring Boot 3 (Java 21)**, cada uno con su propio `Dockerfile`, base de datos dedicada en PostgreSQL, pruebas unitarias JUnit 5 + Mockito y un **API Gateway (Nginx)** en el puerto `8080` para mantener compatibilidad directa con Retrofit en Android.

## Arquitectura de Contenedores (`docker-compose.yml`)
1. **`usuarios-service` (Puerto `8081` — BD `usuariosBD`):**
   - Gestión de autenticación, registro, recuperación de contraseña (`/auth/**`), roles (`administrador` y `socio`) y administración de usuarios (`/admin/usuarios`, `/admin/socios`).
   - Emite tokens JWT firmados con el rol incluido.
2. **`vuelos-operacion-service` (Puerto `8082` — BD `vuelosBD`):**
   - Gestión de vuelos (`/vuelos/**`, `/admin/vuelos/**`), reservas y consentimiento (`/reserva/**`), manifiesto de pasajeros/horómetros (`/operacion/**`) y estadísticas/reportes (`/admin/estadisticas`, `/admin/reportes/vuelos.csv`).
   - Valida tokens JWT de forma desacoplada y se comunica vía REST con `usuarios-service`.
3. **`api-gateway` (Puerto `8080`):**
   - Enruta las peticiones hacia el microservicio correspondiente conservando las mismas rutas para la app móvil Android.
4. **`postgres-db` (Puerto `5432`):**
   - Instancia PostgreSQL 17 con bases de datos separadas (`usuariosBD` y `vuelosBD`).

## Ejecución
```bash
docker compose up --build
```

## Para detenerlos:

```bash
docker compose down
```

## Documentaci�n y Arquitectura
- [Arquitectura Backend en Microservicios](docs/arquitectura-backend.md)

Backend construido con Spring Boot y organizado en microservicios. Usa PostgreSQL para persistencia y Nginx como API Gateway.

Los datos de PostgreSQL se guardan en un volumen de Docker. `docker compose down -v` también elimina ese volumen y los datos almacenados.

## Endpoints principales

Las rutas siguientes se exponen a través del API Gateway en `http://localhost:8080`. Las rutas protegidas requieren un token en el encabezado `Authorization: Bearer <token>`.

### Usuarios y autenticación

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/auth/register` | Registrar una cuenta |
| `POST` | `/auth/login` | Iniciar sesión |
| `GET` | `/auth/me` | Consultar el perfil autenticado |
| `PUT` | `/auth/me` | Modificar el perfil autenticado |
| `POST` | `/auth/recuperar` | Iniciar recuperación de contraseña |
| `POST` | `/auth/restablecer` | Restablecer contraseña |

### Administración de usuarios

Estas rutas requieren permisos de administrador.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/admin/socios` | Listar socios |
| `POST` | `/admin/usuarios` | Crear un usuario |
| `GET` | `/admin/usuarios` | Listar usuarios; admite el parámetro opcional `rol` |
| `PUT` | `/admin/usuarios/{id}` | Editar un usuario |
| `DELETE` | `/admin/usuarios/{id}` | Eliminar un usuario |

### Vuelos

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/vuelos` | Consultar vuelos; admite `fecha` y `disponibles` |
| `GET` | `/vuelos/{id}` | Consultar un vuelo por su identificador |
| `POST` | `/admin/vuelos` | Crear un vuelo; requiere permisos de administrador |
| `PUT` | `/admin/vuelos/{id}` | Editar un vuelo; requiere permisos de administrador |
| `DELETE` | `/admin/vuelos/{id}` | Eliminar un vuelo; requiere permisos de administrador |
| `GET` | `/admin/estadisticas` | Consultar estadísticas; admite `desde` y `hasta` |
| `GET` | `/admin/reportes/vuelos.csv` | Descargar un reporte CSV; requiere permisos de administrador |

### Reservas

Las operaciones de reserva requieren autenticación de socio.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/reserva` | Consultar la reserva del socio |
| `PUT` | `/reserva` | Solicitar una reserva |
| `PUT` | `/reserva/consentimiento` | Actualizar el consentimiento |
| `DELETE` | `/reserva` | Cancelar la reserva |

### Operaciones de vuelo

Estas rutas requieren permisos de administrador.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/operacion/vuelos/{id}/pasajeros` | Consultar pasajeros de un vuelo |
| `POST` | `/operacion/vuelos/{id}/pasajeros` | Registrar un pasajero |
| `PUT` | `/operacion/pasajeros/{id}` | Actualizar un pasajero |
| `DELETE` | `/operacion/pasajeros/{id}` | Eliminar un pasajero |
| `GET` | `/operacion/vuelos/{id}/horometros` | Consultar horómetros |
| `PUT` | `/operacion/vuelos/{id}/horometros` | Actualizar horómetros |
| `POST` | `/operacion/sync` | Sincronizar información de operación |

## Estado de salud

El endpoint `GET /health` está disponible a través del API Gateway. Cada microservicio también ofrece endpoints de salud en su propio puerto.

## Configuración

Las variables de entorno de ejemplo están en `.env.example`. No compartas archivos `.env` ni secretos reales en el repositorio.