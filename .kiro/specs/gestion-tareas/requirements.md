# Documento de Requisitos

## Introducción

Esta funcionalidad permite gestionar tareas académicas dentro del sistema SIA, de forma similar a Google Classroom. Un instructor puede crear tareas asociadas a una ficha específica (identificada por `nombreFicha` en `Programacion`). Los aprendices pertenecientes a esa ficha pueden ver las tareas asignadas y subir sus entregas. El instructor puede revisar las entregas y asignar calificaciones.

## Glosario

- **Sistema**: La aplicación SIA (Spring Boot + Thymeleaf).
- **Instructor**: Usuario con rol instructor registrado en la entidad `Instructor`, que tiene una o más fichas asociadas a través de `Programacion`.
- **Aprendiz**: Usuario con rol aprendiz registrado en la entidad `Aprendiz`, que pertenece a una ficha identificada por el campo `fichaFormacion`.
- **Ficha**: Grupo de formación identificado por el campo `nombreFicha` en la entidad `Programacion`. Un instructor puede tener múltiples fichas.
- **Tarea**: Actividad académica creada por un instructor para una ficha específica, con título, descripción, fecha límite y archivos adjuntos opcionales.
- **Entrega**: Archivo o conjunto de archivos subidos por un aprendiz en respuesta a una tarea.
- **Calificación**: Nota numérica y comentario asignados por el instructor a una entrega.
- **GestorTareas**: Componente del Sistema responsable de crear, listar y gestionar tareas.
- **GestorEntregas**: Componente del Sistema responsable de recibir, almacenar y listar entregas de aprendices.

---

## Requisitos

### Requisito 1: Selección de ficha al crear tarea

**User Story:** Como instructor, quiero seleccionar en qué ficha crear una tarea, para que solo los aprendices de esa ficha la vean.

#### Criterios de Aceptación

1. WHEN el instructor accede al formulario de creación de tarea, THE GestorTareas SHALL mostrar únicamente las fichas asociadas al instructor autenticado, obtenidas desde `Programacion` por `instructor.id`.
2. THE GestorTareas SHALL requerir que el instructor seleccione exactamente una ficha antes de guardar la tarea.
3. IF el instructor no tiene fichas registradas en `Programacion`, THEN THE GestorTareas SHALL mostrar un mensaje indicando que no hay fichas disponibles y deshabilitar el formulario de creación.

---

### Requisito 2: Creación de tarea por el instructor

**User Story:** Como instructor, quiero crear una tarea con título, descripción y fecha límite, para que los aprendices sepan qué deben entregar y cuándo.

#### Criterios de Aceptación

1. THE GestorTareas SHALL requerir un título de máximo 200 caracteres para guardar una tarea.
2. THE GestorTareas SHALL requerir una fecha límite de entrega en formato `yyyy-MM-dd HH:mm` para guardar una tarea.
3. WHERE el instructor adjunta un archivo al crear la tarea, THE GestorTareas SHALL aceptar archivos con extensiones PDF, DOCX, XLSX, PNG y JPG con un tamaño máximo de 10 MB por archivo.
4. WHEN el instructor guarda una tarea válida, THE GestorTareas SHALL persistir la tarea asociada a la ficha seleccionada y al instructor autenticado.
5. IF algún campo obligatorio está vacío al intentar guardar, THEN THE GestorTareas SHALL mostrar un mensaje de error indicando el campo faltante sin perder los datos ya ingresados.

---

### Requisito 3: Visualización de tareas por el aprendiz

**User Story:** Como aprendiz, quiero ver las tareas asignadas a mi ficha, para saber qué actividades debo completar.

#### Criterios de Aceptación

1. WHEN el aprendiz accede a la sección de tareas, THE GestorTareas SHALL mostrar únicamente las tareas cuya ficha coincida con el campo `fichaFormacion` del aprendiz autenticado.
2. THE GestorTareas SHALL mostrar para cada tarea: título, descripción, nombre del instructor, fecha límite y estado de entrega del aprendiz (pendiente, entregada, calificada).
3. WHILE la fecha actual es anterior a la fecha límite de una tarea, THE GestorTareas SHALL mostrar esa tarea con estado "activa".
4. WHEN la fecha actual supera la fecha límite de una tarea y el aprendiz no ha entregado, THE GestorTareas SHALL mostrar esa tarea con estado "vencida".

---

### Requisito 4: Entrega de tarea por el aprendiz

**User Story:** Como aprendiz, quiero subir mi trabajo para una tarea, para que el instructor pueda revisarlo.

#### Criterios de Aceptación

1. WHEN el aprendiz selecciona una tarea activa y sube un archivo, THE GestorEntregas SHALL aceptar archivos con extensiones PDF, DOCX, XLSX, PNG y JPG con un tamaño máximo de 20 MB por entrega.
2. WHEN el aprendiz confirma la entrega, THE GestorEntregas SHALL persistir la entrega asociada al aprendiz autenticado, la tarea y registrar la fecha y hora exacta de entrega.
3. IF el aprendiz ya realizó una entrega para la misma tarea, THEN THE GestorEntregas SHALL permitir reemplazar la entrega anterior siempre que la fecha actual sea anterior a la fecha límite, conservando solo la entrega más reciente.
4. IF el aprendiz intenta entregar después de la fecha límite, THEN THE GestorEntregas SHALL rechazar la entrega y mostrar un mensaje indicando que el plazo ha vencido.
5. IF el archivo subido supera el tamaño máximo o tiene una extensión no permitida, THEN THE GestorEntregas SHALL rechazar el archivo y mostrar un mensaje de error descriptivo.

---

### Requisito 5: Revisión de entregas por el instructor

**User Story:** Como instructor, quiero ver las entregas de los aprendices para una tarea, para poder calificarlas.

#### Criterios de Aceptación

1. WHEN el instructor selecciona una tarea, THE GestorEntregas SHALL mostrar la lista de aprendices de la ficha con su estado de entrega (entregado / pendiente) y la fecha de entrega cuando aplique.
2. THE GestorEntregas SHALL permitir al instructor descargar el archivo de entrega de cada aprendiz.
3. WHEN el instructor asigna una calificación entre 0.0 y 5.0 y un comentario opcional, THE GestorEntregas SHALL persistir la calificación asociada a la entrega del aprendiz.
4. IF el instructor intenta guardar una calificación fuera del rango 0.0 a 5.0, THEN THE GestorEntregas SHALL rechazar la operación y mostrar un mensaje de error.

---

### Requisito 6: Notificación al aprendiz sobre nueva tarea

**User Story:** Como aprendiz, quiero recibir una notificación cuando el instructor publique una nueva tarea en mi ficha, para no perderme ninguna actividad.

#### Criterios de Aceptación

1. WHEN el instructor guarda una tarea nueva, THE Sistema SHALL enviar una notificación a través del mecanismo de WebSocket existente a todos los aprendices cuyo `fichaFormacion` coincida con la ficha de la tarea.
2. THE Sistema SHALL persistir la notificación en la entidad `Notificacion` existente para cada aprendiz destinatario.
3. IF la conexión WebSocket de un aprendiz no está activa al momento de la publicación, THEN THE Sistema SHALL mantener la notificación persistida para que el aprendiz la vea al iniciar sesión.

---

### Requisito 7: Almacenamiento de archivos

**User Story:** Como administrador del sistema, quiero que los archivos de tareas y entregas se almacenen de forma organizada en el servidor, para facilitar su gestión y recuperación.

#### Criterios de Aceptación

1. THE Sistema SHALL almacenar los archivos de tareas en la ruta `uploads/tareas/{idTarea}/` del servidor.
2. THE Sistema SHALL almacenar los archivos de entregas en la ruta `uploads/entregas/{idTarea}/{idAprendiz}/` del servidor.
3. IF ocurre un error al guardar un archivo en disco, THEN THE Sistema SHALL revertir la operación de base de datos correspondiente y retornar un mensaje de error al usuario.
4. THE Sistema SHALL generar un nombre de archivo único usando UUID para evitar colisiones, preservando la extensión original del archivo.
