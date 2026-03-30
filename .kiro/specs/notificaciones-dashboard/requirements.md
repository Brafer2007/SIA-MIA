# Documento de Requisitos: Notificaciones en Dashboards de Instructor y Aprendiz

## Introducción

Este documento define los requisitos para integrar el sistema de notificaciones en tiempo real en los dashboards del instructor y del aprendiz del SIA. La infraestructura base ya existe (`Notificacion`, `NotificacionService`, `NotificationWebSocketHandler`); el objetivo es extenderla para que ambos roles reciban y visualicen notificaciones contextuales en sus paneles, con soporte para persistencia por usuario, conteo de no leídas y despacho desde eventos del dominio (entrega de tareas, inasistencias, incapacidades).

## Glosario

- **SIA**: Sistema de Información del Aprendizaje, la aplicación backend Spring Boot.
- **Notificacion**: Entidad JPA que representa un mensaje de notificación persistido en base de datos.
- **NotificacionService**: Servicio Spring que gestiona la creación, consulta y actualización de notificaciones.
- **NotificationWebSocketHandler**: Componente que gestiona las sesiones WebSocket y envía mensajes en tiempo real a los clientes conectados.
- **SistemaEventos**: Bus de eventos del dominio basado en el patrón Observer que permite emitir y escuchar eventos entre componentes.
- **DashboardController**: Controlador Spring MVC que expone los endpoints REST del dashboard para aprendices e instructores.
- **Dashboard_Frontend**: Código JavaScript del lado del cliente que gestiona la conexión WebSocket y actualiza la interfaz del dashboard.
- **Aprendiz**: Usuario con rol aprendiz en el sistema.
- **Instructor**: Usuario con rol instructor en el sistema.
- **destinatarioId**: Identificador del usuario destinatario de una notificación (idUsuario).
- **Badge**: Indicador visual numérico en el dashboard que muestra la cantidad de notificaciones no leídas.

---

## Requisitos

### Requisito 1: Extensión de la entidad Notificacion para soporte por usuario

**Historia de usuario:** Como desarrollador del SIA, quiero que la entidad `Notificacion` almacene el destinatario y su rol, para que las notificaciones puedan filtrarse y entregarse por usuario específico.

#### Criterios de Aceptación

1. THE Notificacion SHALL almacenar un campo `destinatarioId` de tipo `Long` que identifica al usuario destinatario.
2. THE Notificacion SHALL almacenar un campo `rol` de tipo `String` con valor `"aprendiz"` o `"instructor"` que indica el rol del destinatario.
3. THE NotificacionService SHALL exponer un método `crearParaUsuario(Long destinatarioId, String rol, String mensaje, String tipo)` que persiste una notificación con los campos `destinatarioId` y `rol` correctamente asignados.
4. WHEN se persiste una notificación con `destinatarioId` y `rol`, THE NotificacionService SHALL recuperar esa misma notificación con los valores originales de `destinatarioId` y `rol` intactos.

---

### Requisito 2: Despacho de notificaciones desde eventos del dominio

**Historia de usuario:** Como instructor, quiero recibir una notificación cuando un aprendiz entrega una tarea, para estar al tanto de las entregas sin revisar el listado manualmente.

#### Criterios de Aceptación

1. WHEN un aprendiz entrega una tarea, THE SistemaEventos SHALL emitir un evento de tipo `EntregaTareaEvento` que incluye el identificador de la tarea y el identificador del instructor responsable.
2. WHEN el SistemaEventos emite un `EntregaTareaEvento`, THE NotificacionService SHALL crear y persistir una notificación con `rol = "instructor"` y `destinatarioId` igual al identificador del instructor responsable de la tarea.
3. WHEN un instructor registra una inasistencia para un aprendiz, THE SistemaEventos SHALL emitir un evento de tipo `InasistenciaEvento` que incluye el identificador del aprendiz afectado.
4. WHEN el SistemaEventos emite un `InasistenciaEvento`, THE NotificacionService SHALL crear y persistir una notificación con `rol = "aprendiz"` y `destinatarioId` igual al identificador del aprendiz afectado.
5. WHEN se registra una incapacidad en el sistema, THE SistemaEventos SHALL emitir un evento de tipo `IncapacidadEvento` que incluye el identificador del destinatario y su rol.
6. WHEN el SistemaEventos emite un `IncapacidadEvento`, THE NotificacionService SHALL crear y persistir una notificación con el `destinatarioId` y `rol` especificados en el evento.

---

### Requisito 3: Entrega en tiempo real vía WebSocket

**Historia de usuario:** Como aprendiz o instructor, quiero recibir notificaciones en tiempo real en mi dashboard, para enterarme de eventos relevantes sin necesidad de recargar la página.

#### Criterios de Aceptación

1. WHEN el NotificacionService crea una notificación para un usuario, THE NotificationWebSocketHandler SHALL enviar el mensaje de notificación a la sesión WebSocket activa asociada al `destinatarioId` correspondiente.
2. IF el destinatario no tiene una sesión WebSocket activa al momento de crear la notificación, THEN THE NotificationWebSocketHandler SHALL omitir el envío sin lanzar una excepción ni interrumpir el flujo de persistencia.
3. WHEN un cliente se conecta al endpoint WebSocket del dashboard, THE NotificationWebSocketHandler SHALL registrar la sesión asociándola al `destinatarioId` del usuario autenticado.
4. WHEN un cliente se desconecta del endpoint WebSocket, THE NotificationWebSocketHandler SHALL eliminar la sesión registrada para ese `destinatarioId`.

---

### Requisito 4: Endpoints REST para consulta y gestión de notificaciones

**Historia de usuario:** Como aprendiz o instructor, quiero poder consultar mis notificaciones y marcarlas como leídas desde el dashboard, para gestionar mi bandeja de notificaciones.

#### Criterios de Aceptación

1. WHEN un aprendiz autenticado realiza una petición `GET /dashboard/aprendiz/notificaciones`, THE DashboardController SHALL retornar únicamente las notificaciones cuyo `rol` sea `"aprendiz"` y cuyo `destinatarioId` coincida con el identificador del usuario autenticado.
2. WHEN un instructor autenticado realiza una petición `GET /dashboard/instructor/notificaciones`, THE DashboardController SHALL retornar únicamente las notificaciones cuyo `rol` sea `"instructor"` y cuyo `destinatarioId` coincida con el identificador del usuario autenticado.
3. WHEN un aprendiz autenticado realiza una petición `POST /dashboard/aprendiz/notificaciones/leidas`, THE DashboardController SHALL invocar al NotificacionService para marcar como leídas todas las notificaciones del aprendiz autenticado.
4. WHEN un instructor autenticado realiza una petición `POST /dashboard/instructor/notificaciones/leidas`, THE DashboardController SHALL invocar al NotificacionService para marcar como leídas todas las notificaciones del instructor autenticado.
5. IF un usuario no autenticado intenta acceder a cualquiera de los endpoints de notificaciones, THEN THE DashboardController SHALL retornar una respuesta con código HTTP 401.
6. IF un usuario autenticado intenta acceder a los endpoints del rol contrario, THEN THE DashboardController SHALL retornar una respuesta con código HTTP 403.

---

### Requisito 5: Servicio de conteo y marcado de notificaciones

**Historia de usuario:** Como desarrollador del SIA, quiero que el NotificacionService exponga operaciones de conteo y marcado de leídas, para que los endpoints y el frontend puedan mostrar información precisa sobre notificaciones pendientes.

#### Criterios de Aceptación

1. THE NotificacionService SHALL exponer un método `contarNoLeidas(Long destinatarioId, String rol)` que retorna el número de notificaciones con `leida = false` para el usuario y rol especificados.
2. WHEN se invoca `contarNoLeidas` para un usuario, THE NotificacionService SHALL retornar un valor igual al número de registros persistidos con `leida = false`, `destinatarioId` y `rol` coincidentes.
3. THE NotificacionService SHALL exponer un método `marcarTodasLeidas(Long destinatarioId, String rol)` que actualiza a `leida = true` todas las notificaciones del usuario y rol especificados.
4. WHEN se invoca `marcarTodasLeidas` para un usuario, THE NotificacionService SHALL actualizar el campo `leida` a `true` en todas las notificaciones del usuario, de modo que una llamada posterior a `contarNoLeidas` para ese mismo usuario retorne `0`.

---

### Requisito 6: Actualización del dashboard en el frontend

**Historia de usuario:** Como aprendiz o instructor, quiero que el badge de notificaciones y el panel de mi dashboard se actualicen automáticamente al recibir nuevas notificaciones, para tener visibilidad inmediata de eventos pendientes.

#### Criterios de Aceptación

1. WHEN el Dashboard_Frontend recibe un mensaje de notificación a través de la conexión WebSocket, THE Dashboard_Frontend SHALL incrementar el contador del badge de notificaciones no leídas en la interfaz.
2. WHEN el usuario abre el panel de notificaciones en el dashboard, THE Dashboard_Frontend SHALL mostrar la lista de notificaciones obtenida del endpoint REST correspondiente a su rol.
3. WHEN el usuario ejecuta la acción de marcar notificaciones como leídas, THE Dashboard_Frontend SHALL invocar el endpoint `POST .../notificaciones/leidas` y actualizar el badge a `0`.
4. WHEN la página del dashboard se carga inicialmente, THE Dashboard_Frontend SHALL consultar el conteo de no leídas vía el endpoint REST e inicializar el badge con el valor retornado.

---

## Propiedades de Corrección

*Una propiedad es una característica o comportamiento que debe mantenerse verdadero en todas las ejecuciones válidas del sistema. Las propiedades sirven como puente entre las especificaciones legibles por humanos y las garantías de corrección verificables automáticamente.*

### Propiedad 1: Round-trip de persistencia de notificación por usuario

*Para cualquier* combinación de `destinatarioId` y `rol`, crear una notificación con esos valores y luego recuperarla de la base de datos debe producir un objeto con los mismos valores de `destinatarioId` y `rol`.

**Valida: Requisitos 1.3, 1.4**

---

### Propiedad 2: Entrega de tarea genera notificación para el instructor correcto

*Para cualquier* entrega de tarea válida, el instructor responsable debe tener exactamente una notificación nueva con `rol = "instructor"` y `destinatarioId` igual a su identificador, y ningún otro instructor debe recibir esa notificación.

**Valida: Requisitos 2.1, 2.2**

---

### Propiedad 3: Registro de inasistencia genera notificación para el aprendiz correcto

*Para cualquier* registro de inasistencia, el aprendiz afectado debe tener exactamente una notificación nueva con `rol = "aprendiz"` y `destinatarioId` igual a su identificador.

**Valida: Requisitos 2.3, 2.4**

---

### Propiedad 4: Los endpoints REST retornan solo notificaciones del usuario autenticado

*Para cualquier* usuario autenticado (aprendiz o instructor), el endpoint GET de notificaciones debe retornar únicamente notificaciones donde `destinatarioId` coincide con el identificador del usuario y `rol` coincide con su rol, nunca notificaciones de otros usuarios.

**Valida: Requisitos 4.1, 4.2**

---

### Propiedad 5: Marcar como leídas deja conteo en cero

*Para cualquier* usuario con notificaciones no leídas, invocar `marcarTodasLeidas` seguido de `contarNoLeidas` debe retornar `0`.

**Valida: Requisitos 5.3, 5.4, 4.3, 4.4**

---

### Propiedad 6: El badge refleja el conteo real de no leídas

*Para cualquier* estado del sistema, el valor numérico mostrado en el badge del dashboard debe ser igual al valor retornado por `contarNoLeidas` para el usuario autenticado en ese momento.

**Valida: Requisitos 6.1, 6.4**
