# Plan de Implementación: Gestión de Tareas

## Visión General

Implementación incremental de la funcionalidad de gestión de tareas académicas en el sistema SIA (Spring Boot 3.3.4 + Thymeleaf + MySQL + Java 17). Se crean las entidades `Tarea` y `EntregaTarea`, los repositorios, servicios, controladores y vistas necesarios, integrando el WebSocket existente para notificaciones.

## Tareas

- [x] 1. Agregar dependencia jqwik y crear entidades JPA
  - Agregar `net.jqwik:jqwik:1.8.1` con scope `test` en `pom.xml`
  - Crear `src/main/java/com/example/SIA/entity/Tarea.java` con los campos: `id`, `titulo` (max 200), `descripcion`, `fechaLimite` (`LocalDateTime`), `nombreFicha`, `instructor` (`@ManyToOne`), `rutaArchivo`, `fechaCreacion`
  - Crear `src/main/java/com/example/SIA/entity/EntregaTarea.java` con los campos: `id`, `tarea` (`@ManyToOne`), `aprendiz` (`@ManyToOne`), `rutaArchivo`, `fechaEntrega`, `nota` (Double), `comentarioInstructor`, `fechaCalificacion`; agregar `@UniqueConstraint(columnNames = {"id_tarea", "id_aprendiz"})`
  - _Requisitos: 2.1, 2.2, 2.4, 4.2, 4.3, 5.3_

- [x] 2. Crear repositorios y query adicional en ProgramacionRepository
  - [x] 2.1 Crear `TareaRepository` extendiendo `JpaRepository<Tarea, Long>` con métodos: `findByNombreFicha`, `findByInstructor_Id`, `findByNombreFichaAndInstructor_Id`
    - _Requisitos: 1.1, 3.1_
  - [x] 2.2 Crear `EntregaTareaRepository` extendiendo `JpaRepository<EntregaTarea, Long>` con métodos: `findByTarea_IdAndAprendiz_IdAprendiz`, `findByTarea_Id`
    - _Requisitos: 4.2, 4.3, 5.1_
  - [x] 2.3 Agregar al `ProgramacionRepository` existente el método `findFichasByInstructorId` con `@Query("SELECT DISTINCT p.nombreFicha FROM Programacion p WHERE p.instructor.id = :idInstructor")`
    - _Requisitos: 1.1, 1.3_

- [x] 3. Crear DTOs
  - Crear `TareaRequest.java` con campos: `titulo` (`@NotBlank @Size(max=200)`), `descripcion`, `fechaLimite` (`@NotNull LocalDateTime`), `nombreFicha` (`@NotBlank`); incluir getters/setters sin Lombok
  - Crear `TareaAprendizDTO.java` con campos: `idTarea`, `titulo`, `descripcion`, `nombreInstructor`, `fechaLimite`, `estadoEntrega` (String), `nota`, `comentarioInstructor`, `tieneArchivoTarea`; incluir getters/setters
  - Crear `EntregaResumenDTO.java` con campos: `idAprendiz`, `nombreAprendiz`, `estadoEntrega`, `fechaEntrega`, `idEntrega`, `rutaArchivo`, `nota`, `comentario`; incluir getters/setters
  - _Requisitos: 2.1, 2.2, 3.2, 5.1_

- [x] 4. Implementar ArchivoService
  - [x] 4.1 Crear `src/main/java/com/example/SIA/service/ArchivoService.java` con los métodos:
    - `validarArchivo(MultipartFile archivo, long maxBytes)`: lanza `IllegalArgumentException` si la extensión no está en `{pdf, docx, xlsx, png, jpg}` o si el tamaño supera `maxBytes`
    - `guardarArchivoTarea(Long idTarea, MultipartFile archivo)`: guarda en `uploads/tareas/{idTarea}/` con nombre UUID + extensión original; retorna ruta relativa
    - `guardarArchivoEntrega(Long idTarea, Integer idAprendiz, MultipartFile archivo)`: guarda en `uploads/entregas/{idTarea}/{idAprendiz}/` con nombre UUID; retorna ruta relativa
    - `eliminarArchivo(String rutaRelativa)`: elimina el archivo del disco si existe
    - _Requisitos: 2.3, 4.1, 4.5, 7.1, 7.2, 7.4_
  - [ ]* 4.2 Escribir prueba unitaria para `ArchivoService`
    - Casos: extensión permitida aceptada, extensión rechazada, tamaño exacto al límite aceptado, tamaño superado rechazado, nombre UUID generado con extensión correcta
    - _Requisitos: 2.3, 4.5, 7.4_
  - [ ]* 4.3 Escribir prueba de propiedad P3 para `ArchivoService.validarArchivo`
    - **Propiedad 3: Validación de archivos adjuntos**
    - **Valida: Requisitos 2.3, 4.1, 4.5**
  - [ ]* 4.4 Escribir prueba de propiedad P10 para `ArchivoService.guardarArchivoTarea`
    - **Propiedad 10: Round-trip de almacenamiento de archivos**
    - **Valida: Requisitos 7.1, 7.2, 7.4**

- [x] 5. Implementar TareaService
  - [x] 5.1 Crear `src/main/java/com/example/SIA/service/TareaService.java` con los métodos:
    - `obtenerFichasDeInstructor(Integer idInstructor)`: delega a `ProgramacionRepository.findFichasByInstructorId`
    - `listarPorInstructor(Integer idInstructor)`: delega a `TareaRepository.findByInstructor_Id`
    - `crearTarea(TareaRequest request, Integer idInstructor, MultipartFile archivo)`: valida campos, guarda archivo si presente, persiste `Tarea`, dispara notificaciones
    - `listarEntregasDeTarea(Long idTarea, Integer idInstructor)`: verifica que la tarea pertenece al instructor, obtiene aprendices de la ficha y sus entregas, retorna lista de `EntregaResumenDTO`
    - `calificar(Long idEntrega, Double nota, String comentario, Integer idInstructor)`: valida rango [0.0, 5.0], persiste calificación
    - _Requisitos: 1.1, 1.3, 2.1, 2.2, 2.4, 5.1, 5.3, 5.4_
  - [ ]* 5.2 Escribir prueba unitaria para `TareaService.crearTarea`
    - Casos: título vacío rechazado, fecha límite nula rechazada, tarea válida persistida
    - _Requisitos: 2.1, 2.2, 2.5_
  - [ ]* 5.3 Escribir prueba de propiedad P1 para `obtenerFichasDeInstructor`
    - **Propiedad 1: Solo fichas del instructor autenticado**
    - **Valida: Requisito 1.1**
  - [ ]* 5.4 Escribir prueba de propiedad P2 para `crearTarea` con campos inválidos
    - **Propiedad 2: Validación de campos obligatorios de tarea**
    - **Valida: Requisitos 2.1, 2.2, 2.5**
  - [ ]* 5.5 Escribir prueba de propiedad P8 para `calificar` con nota fuera de rango
    - **Propiedad 8: Calificación dentro del rango válido**
    - **Valida: Requisitos 5.3, 5.4**

- [x] 6. Implementar lógica de notificaciones en TareaService
  - Inyectar `NotificationWebSocketHandler` y `NotificacionRepository` en `TareaService`
  - En `crearTarea`, después de persistir la tarea: buscar aprendices por `fichaFormacion`, crear y persistir una `Notificacion` por aprendiz, llamar a `notificarAprendicesDeFicha(ficha, notificacionDTO)`
  - _Requisitos: 6.1, 6.2, 6.3_
  - [ ]* 6.1 Escribir prueba de propiedad P9 para notificaciones al crear tarea
    - **Propiedad 9: Notificación a todos los aprendices de la ficha**
    - **Valida: Requisitos 6.1, 6.2**

- [x] 7. Implementar EntregaService
  - [x] 7.1 Crear `src/main/java/com/example/SIA/service/EntregaService.java` con los métodos:
    - `listarTareasParaAprendiz(String fichaFormacion, Integer idAprendiz)`: obtiene tareas de la ficha, calcula estado (`PENDIENTE`, `ENTREGADA`, `CALIFICADA`, `VENCIDA`) para cada una, retorna lista de `TareaAprendizDTO`
    - `entregar(Long idTarea, Integer idAprendiz, MultipartFile archivo)`: valida plazo, valida archivo (max 20 MB), reemplaza entrega existente si la hay, persiste `EntregaTarea`
    - _Requisitos: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 4.5_
  - [ ]* 7.2 Escribir prueba unitaria para `EntregaService`
    - Casos: entrega fuera de plazo rechazada, reemplazo de entrega existente, estados PENDIENTE/VENCIDA/ENTREGADA/CALIFICADA calculados correctamente
    - _Requisitos: 3.3, 3.4, 4.3, 4.4_
  - [ ]* 7.3 Escribir prueba de propiedad P4 para `listarTareasParaAprendiz`
    - **Propiedad 4: Aislamiento de tareas por ficha**
    - **Valida: Requisito 3.1**
  - [ ]* 7.4 Escribir prueba de propiedad P5 para cálculo de estado de tarea
    - **Propiedad 5: Estado de tarea coherente con fecha límite**
    - **Valida: Requisitos 3.3, 3.4**
  - [ ]* 7.5 Escribir prueba de propiedad P6 para reemplazo de entrega
    - **Propiedad 6: Entrega reemplaza la anterior dentro del plazo**
    - **Valida: Requisito 4.3**
  - [ ]* 7.6 Escribir prueba de propiedad P7 para rechazo de entrega fuera de plazo
    - **Propiedad 7: Rechazo de entrega fuera de plazo**
    - **Valida: Requisito 4.4**

- [x] 8. Checkpoint — Verificar que todos los tests pasan
  - Asegurarse de que todos los tests unitarios y de propiedad implementados hasta aquí pasan. Consultar al usuario si hay dudas.

- [x] 9. Implementar TareaController (rutas del instructor)
  - Crear `src/main/java/com/example/SIA/controller/TareaController.java` con las rutas:
    - `GET /instructor/tareas` → lista tareas del instructor autenticado (sesión HTTP)
    - `GET /instructor/tareas/nueva` → formulario de creación con lista de fichas
    - `POST /instructor/tareas/nueva` → guarda tarea; en error redirige al formulario con mensaje
    - `GET /instructor/tareas/{id}/entregas` → panel de entregas de la tarea
    - `POST /instructor/tareas/{id}/calificar/{idEntrega}` → guarda calificación
  - Leer `idInstructor` desde `HttpSession` (clave `"idInstructor"` o equivalente del proyecto)
  - Usar `Model` para pasar datos a las vistas Thymeleaf
  - _Requisitos: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 2.5, 5.1, 5.2, 5.3, 5.4_

- [x] 10. Implementar EntregaController (rutas del aprendiz)
  - Crear `src/main/java/com/example/SIA/controller/EntregaController.java` con las rutas:
    - `GET /aprendiz/tareas` → lista tareas de la ficha del aprendiz autenticado
    - `GET /aprendiz/tareas/{id}` → detalle de tarea + formulario de entrega
    - `POST /aprendiz/tareas/{id}/entregar` → sube entrega; maneja errores de plazo y archivo
  - Leer `idAprendiz` y `fichaFormacion` desde `HttpSession`
  - _Requisitos: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 11. Crear vistas Thymeleaf del instructor
  - [x] 11.1 Crear `src/main/resources/templates/instructor/tareas/lista.html`
    - Tabla con columnas: título, ficha, fecha límite, número de entregas, enlace a entregas
    - Enlace a formulario de nueva tarea
    - _Requisitos: 2.4_
  - [x] 11.2 Crear `src/main/resources/templates/instructor/tareas/nueva.html`
    - Formulario con campos: título, descripción, fecha límite (`datetime-local`), selector de ficha (`<select>`), archivo adjunto opcional
    - Mostrar mensaje de error si no hay fichas disponibles (deshabilitar formulario)
    - Mostrar mensajes de validación por campo
    - _Requisitos: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.5_
  - [x] 11.3 Crear `src/main/resources/templates/instructor/tareas/entregas.html`
    - Tabla de aprendices con columnas: nombre, estado, fecha entrega, enlace descarga, campos nota y comentario, botón guardar calificación
    - _Requisitos: 5.1, 5.2, 5.3, 5.4_

- [x] 12. Crear vistas Thymeleaf del aprendiz
  - [x] 12.1 Crear `src/main/resources/templates/aprendiz/tareas/lista.html`
    - Tarjetas o tabla con: título, instructor, fecha límite, estado (badge de color), enlace a detalle
    - _Requisitos: 3.1, 3.2, 3.3, 3.4_
  - [x] 12.2 Crear `src/main/resources/templates/aprendiz/tareas/detalle.html`
    - Detalle de la tarea (título, descripción, fecha límite, archivo del instructor si existe)
    - Formulario de entrega (solo visible si la tarea está activa)
    - Mostrar entrega actual y nota/comentario si ya fue calificada
    - Mensaje de plazo vencido si corresponde
    - _Requisitos: 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 13. Manejar atomicidad archivo-base de datos en servicios
  - En `TareaService.crearTarea` y `EntregaService.entregar`: envolver la operación en try/catch; si la persistencia JPA falla después de guardar el archivo, invocar `ArchivoService.eliminarArchivo()` en el bloque `catch`
  - Anotar los métodos de servicio con `@Transactional`
  - _Requisitos: 7.3_
  - [ ]* 13.1 Escribir prueba de propiedad P11 para atomicidad archivo-BD
    - **Propiedad 11: Atomicidad archivo-base de datos**
    - **Valida: Requisito 7.3**

- [x] 14. Checkpoint final — Verificar integración completa
  - Asegurarse de que todos los tests pasan y que los flujos instructor→tarea→notificación y aprendiz→entrega→calificación están conectados de extremo a extremo. Consultar al usuario si hay dudas.

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido.
- Cada tarea referencia los requisitos específicos para trazabilidad.
- No usar Lombok: todos los modelos deben tener getters/setters manuales.
- La autenticación se lee desde `HttpSession`; no se modifica `SecurityConfig`.
- jqwik requiere ser agregado a `pom.xml` antes de escribir las pruebas de propiedad.
- Los archivos se sirven vía `/uploads/**` ya configurado en `WebConfig`.
