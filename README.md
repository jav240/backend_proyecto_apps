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
