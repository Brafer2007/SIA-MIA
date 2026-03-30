# Plan de Implementación: Notificaciones en Dashboards de Instructor y Aprendiz

## Visión General

Extender la infraestructura existente de notificaciones para soportar destinatarios por usuario y rol, emitir notificaciones desde eventos del dominio, exponerlas vía REST y actualizarlas en tiempo real en los dashboards de aprendiz e instructor.

## Tareas

- [ ] 1. Extender la entidad `Notificacion` y el repositorio
  - [ ] 1.1 Agregar campos `destinatarioId` (Long) y `rol` (String) a `Notificacion.java` con sus getters/setters
    - Modificar `src/main/java/com/example/SIA/entity/Notificacion.java`
    - _Requisitos: 1.1, 1.2_

  - [ ] 1.2 Agregar queries en `NotificacionRepository` para buscar por `destinatarioId` y `rol`
    - Agregar `findByDestinatarioIdAndRol(Long destinatarioId, String rol)`
    - Agregar `countByDestinatarioIdAndRolAndLeidaFalse(Long destinatarioId, String rol)`
    - Agregar `findByDestinatarioIdAndRolAndLeidaFalse(Long destinatarioId, String rol)` para marcar leídas
    - Modificar `src/main/java/com/example/SIA/repository/NotificacionRepository.java`
    - _Requisitos: 1.1, 1.2, 5.1, 5.3_

  - [ ]* 1.3 Escribir test de propiedad: Round-trip de persistencia de notificación por usuario
    - **Propiedad 1: Round-trip de persistencia de notificación por usuario**
    - Para cualquier `destinatarioId` y `rol`, crear y recuperar debe producir los mismos valores
    - **Valida: Requisitos 1.3, 1.4**
    - Crear `src/test/java/com/example/SIA/NotificacionRoundTripPropertyTest.java`

- [ ] 2. Extender `NotificacionService` con operaciones por usuario
  - [ ] 2.1 Implementar `crearParaUsuario(Long destinatarioId, String rol, String mensaje, String tipo)` en `NotificacionService`
    - Persiste la notificación con `destinatarioId` y `rol` asignados
    - Modificar `src/main/java/com/example/SIA/service/NotificacionService.java`
    - _Requisitos: 1.3, 1.4_

  - [ ] 2.2 Implementar `contarNoLeidas(Long destinatarioId, String rol)` en `NotificacionService`
    - Retorna el número de notificaciones con `leida = false` para el usuario y rol dados
    - _Requisitos: 5.1, 5.2_

  - [ ] 2.3 Implementar `marcarTodasLeidas(Long destinatarioId, String rol)` en `NotificacionService`
    - Actualiza `leida = true` en todas las notificaciones del usuario y rol dados
    - _Requisitos: 5.3, 5.4_

  - [ ]* 2.4 Escribir test de propiedad: Marcar como leídas deja conteo en cero
    - **Propiedad 5: Marcar como leídas deja conteo en cero**
    - Para cualquier usuario con notificaciones no leídas, `marcarTodasLeidas` seguido de `contarNoLeidas` debe retornar `0`
    - **Valida: Requisitos 5.3, 5.4**
    - Crear `src/test/java/com/example/SIA/NotificacionConteoPropertyTest.java`

- [ ] 3. Actualizar `NotificationWebSocketHandler` para envío por `destinatarioId`
  - [ ] 3.1 Modificar el registro de sesiones para asociar `destinatarioId` al conectar y desconectar
    - Al conectar: extraer `destinatarioId` del usuario autenticado y registrar la sesión en un `Map<Long, WebSocketSession>`
    - Al desconectar: eliminar la sesión del mapa
    - Modificar `src/main/java/com/example/SIA/config/NotificationWebSocketHandler.java`
    - _Requisitos: 3.3, 3.4_

  - [ ] 3.2 Agregar método `notificarUsuario(Long destinatarioId, NotificacionDTO dto)` que envía el mensaje JSON a la sesión activa del destinatario
    - Si no hay sesión activa para ese `destinatarioId`, omitir sin lanzar excepción
    - _Requisitos: 3.1, 3.2_

- [ ] 4. Crear eventos de dominio y sus observadores
  - [ ] 4.1 Crear `EntregaTareaEvento` en el paquete `observer`
    - Campos: `tareaId` (Long), `instructorId` (Long), `aprendizId` (Long), `nombreAprendiz` (String)
    - Extender `Evento`
    - Crear `src/main/java/com/example/SIA/observer/EntregaTareaEvento.java`
    - _Requisitos: 2.1_

  - [ ] 4.2 Crear `InasistenciaEvento` en el paquete `observer`
    - Campos: `aprendizId` (Long), `nombreAprendiz` (String), `fecha` (String)
    - Extender `Evento`
    - Crear `src/main/java/com/example/SIA/observer/InasistenciaEvento.java`
    - _Requisitos: 2.3_

  - [ ] 4.3 Crear `IncapacidadEvento` en el paquete `observer`
    - Campos: `destinatarioId` (Long), `rol` (String), `mensaje` (String)
    - Extender `Evento`
    - Crear `src/main/java/com/example/SIA/observer/IncapacidadEvento.java`
    - _Requisitos: 2.5_

  - [ ] 4.4 Crear `NotificacionEventoObservador` como `@Component` que implementa `ObservadorEvento`
    - Escucha `EntregaTareaEvento`, `InasistenciaEvento` e `IncapacidadEvento`
    - Llama a `NotificacionService.crearParaUsuario(...)` y `NotificationWebSocketHandler.notificarUsuario(...)` según el tipo de evento
    - Se registra en `SistemaEventos` al inicializarse (`@PostConstruct`)
    - Crear `src/main/java/com/example/SIA/observer/NotificacionEventoObservador.java`
    - _Requisitos: 2.2, 2.4, 2.6_

  - [ ]* 4.5 Escribir test de propiedad: Entrega de tarea genera notificación para el instructor correcto
    - **Propiedad 2: Entrega de tarea genera notificación para el instructor correcto**
    - Para cualquier entrega válida, solo el instructor responsable recibe la notificación con `rol = "instructor"`
    - **Valida: Requisitos 2.1, 2.2**
    - Crear `src/test/java/com/example/SIA/EntregaTareaNotificacionPropertyTest.java`

  - [ ]* 4.6 Escribir test de propiedad: Inasistencia genera notificación para el aprendiz correcto
    - **Propiedad 3: Registro de inasistencia genera notificación para el aprendiz correcto**
    - Para cualquier inasistencia, solo el aprendiz afectado recibe la notificación con `rol = "aprendiz"`
    - **Valida: Requisitos 2.3, 2.4**

- [ ] 5. Checkpoint — Verificar que todos los tests pasen
  - Asegurarse de que todos los tests pasen hasta este punto; consultar al usuario si surgen dudas.

- [ ] 6. Integrar emisión de eventos en controladores y servicios existentes
  - [ ] 6.1 Emitir `EntregaTareaEvento` desde `EntregaController` o `TareaService` al registrar una entrega
    - Obtener `instructorId` de la tarea asociada y emitir el evento vía `SistemaEventos.emitir(...)`
    - Modificar `src/main/java/com/example/SIA/controller/EntregaController.java` o `src/main/java/com/example/SIA/service/TareaService.java`
    - _Requisitos: 2.1, 2.2_

  - [ ] 6.2 Emitir `InasistenciaEvento` desde `AsistenciaController` al registrar una inasistencia
    - Obtener `aprendizId` del registro y emitir el evento vía `SistemaEventos.emitir(...)`
    - Modificar `src/main/java/com/example/SIA/controller/AsistenciaController.java`
    - _Requisitos: 2.3, 2.4_

  - [ ] 6.3 Emitir `IncapacidadEvento` desde `IncapacidadController` al registrar una incapacidad
    - Determinar `destinatarioId` y `rol` según la lógica de negocio y emitir el evento
    - Modificar `src/main/java/com/example/SIA/controller/IncapacidadController.java`
    - _Requisitos: 2.5, 2.6_

- [ ] 7. Agregar endpoints REST en `DashboardController`
  - [ ] 7.1 Implementar `GET /dashboard/aprendiz/notificaciones` que retorna las notificaciones del aprendiz autenticado
    - Extraer `destinatarioId` del usuario en sesión; retornar solo notificaciones con `rol = "aprendiz"` y `destinatarioId` coincidente
    - Retornar HTTP 401 si no autenticado, HTTP 403 si el rol no coincide
    - Modificar `src/main/java/com/example/SIA/controller/DashboardController.java`
    - _Requisitos: 4.1, 4.5, 4.6_

  - [ ] 7.2 Implementar `GET /dashboard/instructor/notificaciones` que retorna las notificaciones del instructor autenticado
    - Misma lógica que 7.1 pero para `rol = "instructor"`
    - _Requisitos: 4.2, 4.5, 4.6_

  - [ ] 7.3 Implementar `POST /dashboard/aprendiz/notificaciones/leidas` que marca todas las notificaciones del aprendiz como leídas
    - Invocar `NotificacionService.marcarTodasLeidas(destinatarioId, "aprendiz")`
    - _Requisitos: 4.3_

  - [ ] 7.4 Implementar `POST /dashboard/instructor/notificaciones/leidas` que marca todas las notificaciones del instructor como leídas
    - Invocar `NotificacionService.marcarTodasLeidas(destinatarioId, "instructor")`
    - _Requisitos: 4.4_

  - [ ]* 7.5 Escribir test de propiedad: Los endpoints REST retornan solo notificaciones del usuario autenticado
    - **Propiedad 4: Los endpoints REST retornan solo notificaciones del usuario autenticado**
    - Para cualquier usuario autenticado, el endpoint GET nunca retorna notificaciones de otros usuarios
    - **Valida: Requisitos 4.1, 4.2**
    - Crear `src/test/java/com/example/SIA/DashboardNotificacionesPropertyTest.java`

- [ ] 8. Actualizar el frontend de los dashboards
  - [ ] 8.1 Agregar badge de notificaciones no leídas en `dashboardAprendiz.html`
    - Elemento HTML con id `notif-badge` que muestra el conteo
    - Al cargar la página: `GET /dashboard/aprendiz/notificaciones` para inicializar el badge
    - _Requisitos: 6.2, 6.4_

  - [ ] 8.2 Agregar panel de notificaciones y lógica WebSocket en `dashboardAprendiz.html`
    - Conectar al endpoint WebSocket del dashboard; al recibir mensaje incrementar el badge
    - Panel desplegable que lista las notificaciones; botón "Marcar como leídas" que llama `POST /dashboard/aprendiz/notificaciones/leidas` y resetea el badge a `0`
    - Modificar `src/main/resources/templates/aprendiz/dashboardAprendiz.html`
    - _Requisitos: 6.1, 6.2, 6.3, 6.4_

  - [ ] 8.3 Agregar badge de notificaciones no leídas en `dashboardInstructor.html`
    - Misma lógica que 8.1 pero apuntando a `/dashboard/instructor/notificaciones`
    - _Requisitos: 6.2, 6.4_

  - [ ] 8.4 Agregar panel de notificaciones y lógica WebSocket en `dashboardInstructor.html`
    - Misma lógica que 8.2 pero apuntando a los endpoints del instructor
    - Modificar `src/main/resources/templates/instructor/dashboardInstructor.html`
    - _Requisitos: 6.1, 6.2, 6.3, 6.4_

  - [ ]* 8.5 Escribir test de propiedad: El badge refleja el conteo real de no leídas
    - **Propiedad 6: El badge refleja el conteo real de no leídas**
    - El valor del badge debe ser igual al retornado por `contarNoLeidas` para el usuario autenticado
    - **Valida: Requisitos 6.1, 6.4**

- [ ] 9. Checkpoint final — Verificar que todos los tests pasen
  - Asegurarse de que todos los tests pasen; consultar al usuario si surgen dudas.

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia requisitos específicos para trazabilidad
- Los checkpoints garantizan validación incremental
- Los tests de propiedad validan invariantes universales de corrección
- Los tests unitarios validan ejemplos concretos y casos borde
