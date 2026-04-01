# Documento de Requisitos

## Introducción

Esta feature hace responsive los 11 archivos HTML de dashboards y páginas del proyecto SIA (Spring Boot + Thymeleaf). En pantallas móviles (≤768px) el sidebar queda oculto por defecto y se muestra u oculta mediante un botón hamburguesa con overlay oscuro de fondo. El diseño de escritorio no se modifica. No hay cambios en el backend ni en los modelos de datos.

## Glosario

- **Sidebar**: Panel de navegación lateral fijo de 250px de ancho presente en todos los dashboards.
- **Hamburger_Button**: Botón fijo en la esquina superior izquierda, visible solo en móvil, que abre y cierra el sidebar.
- **Overlay**: Capa semitransparente oscura que cubre el contenido cuando el sidebar está abierto en móvil.
- **Breakpoint_Movil**: Ancho de pantalla de 768px o menos, que activa el comportamiento responsive.
- **Layout_Escritorio**: Disposición original con sidebar visible y contenido con `margin-left: 270px`, activa en pantallas mayores a 768px.
- **Sistema**: El conjunto de archivos HTML/CSS/JS del proyecto SIA afectados por esta feature.
- **Archivo_Afectado**: Cualquiera de los 11 archivos HTML listados en el diseño.

---

## Requisitos

### Requisito 1: Sidebar oculto por defecto en móvil

**User Story:** Como usuario que accede desde un dispositivo móvil, quiero que el sidebar no bloquee el contenido al cargar la página, para poder ver la información principal de inmediato.

#### Criterios de Aceptación

1. WHILE el viewport es ≤768px, THE Sistema SHALL renderizar el sidebar con `transform: translateX(-100%)` de modo que quede fuera de la pantalla visible.
2. WHILE el viewport es ≤768px, THE Sistema SHALL mostrar el contenido principal ocupando el 100% del ancho disponible con `margin-left: 0`.
3. WHILE el viewport es >768px, THE Sistema SHALL mantener el Layout_Escritorio original sin ninguna modificación.

---

### Requisito 2: Botón hamburguesa

**User Story:** Como usuario móvil, quiero un botón visible para abrir el menú de navegación, para poder acceder al sidebar cuando lo necesite.

#### Criterios de Aceptación

1. WHILE el viewport es ≤768px, THE Hamburger_Button SHALL estar visible en posición fija en la esquina superior izquierda (`top: 12px`, `left: 12px`, `z-index: 200`).
2. WHILE el viewport es >768px, THE Hamburger_Button SHALL permanecer oculto (`display: none`).
3. THE Hamburger_Button SHALL incluir el atributo `aria-label="Abrir menú"` para accesibilidad.
4. WHEN el usuario hace clic en el Hamburger_Button, THE Sistema SHALL invocar la función `toggleSidebar()`.

---

### Requisito 3: Apertura del sidebar en móvil

**User Story:** Como usuario móvil, quiero que al pulsar el botón hamburguesa el sidebar aparezca con una animación suave, para tener una experiencia de navegación fluida.

#### Criterios de Aceptación

1. WHEN el usuario hace clic en el Hamburger_Button y el sidebar no tiene la clase `open`, THE Sistema SHALL añadir la clase `open` al sidebar y la clase `active` al Overlay.
2. WHEN la clase `open` es añadida al sidebar, THE Sistema SHALL deslizar el sidebar desde la izquierda mediante `transform: translateX(0)` con una transición de 0.3s.
3. WHEN la clase `active` es añadida al Overlay, THE Sistema SHALL mostrar el Overlay con `background: rgba(0,0,0,0.5)` cubriendo toda la pantalla (`position: fixed`, `width: 100%`, `height: 100%`, `z-index: 150`).

---

### Requisito 4: Cierre del sidebar en móvil

**User Story:** Como usuario móvil, quiero poder cerrar el sidebar tocando el overlay o el botón hamburguesa, para volver al contenido principal fácilmente.

#### Criterios de Aceptación

1. WHEN el usuario hace clic en el Overlay, THE Sistema SHALL invocar `closeSidebar()`, removiendo la clase `open` del sidebar y la clase `active` del Overlay.
2. WHEN el usuario hace clic en el Hamburger_Button y el sidebar tiene la clase `open`, THE Sistema SHALL remover la clase `open` del sidebar y la clase `active` del Overlay.
3. WHEN `closeSidebar()` es invocada, THE Sistema SHALL garantizar que el sidebar no tenga la clase `open` y el Overlay no tenga la clase `active`, independientemente del estado previo.

---

### Requisito 5: Consistencia entre los 11 archivos afectados

**User Story:** Como desarrollador, quiero que todos los dashboards tengan el mismo comportamiento responsive, para mantener una experiencia de usuario uniforme en toda la aplicación.

#### Criterios de Aceptación

1. THE Sistema SHALL aplicar el bloque CSS responsive (`.hamburger-btn`, `.sidebar-overlay`, `@media (max-width: 768px)`) a cada uno de los 11 Archivos_Afectados.
2. THE Sistema SHALL insertar el HTML del Hamburger_Button inmediatamente antes del elemento `<div class="sidebar">` o `<aside class="sidebar">` en cada Archivo_Afectado.
3. THE Sistema SHALL insertar el HTML del Overlay inmediatamente después del cierre del sidebar en cada Archivo_Afectado.
4. THE Sistema SHALL añadir las funciones `toggleSidebar()` y `closeSidebar()` en el bloque `<script>` de cada Archivo_Afectado.
5. WHERE el Archivo_Afectado usa la clase `.main-content` en lugar de `.content`, THE Sistema SHALL incluir ambas clases en la regla CSS del media query: `.content, .main-content { margin-left: 0 !important; }`.

---

### Requisito 6: Migración del media query existente en dashboardAdministrador

**User Story:** Como desarrollador, quiero que el media query antiguo de dashboardAdministrador sea reemplazado por la solución unificada, para evitar conflictos de estilos y mantener el breakpoint consistente en 768px.

#### Criterios de Aceptación

1. WHEN el Archivo_Afectado es `dashboardAdministrador.html`, THE Sistema SHALL reemplazar el bloque `@media (max-width: 720px)` existente por el bloque CSS responsive unificado con breakpoint `768px`.
2. IF el bloque `@media (max-width: 720px)` contiene reglas `display: none` sobre el sidebar, THEN THE Sistema SHALL eliminar dichas reglas y sustituirlas por `transform: translateX(-100%)`.

---

### Requisito 7: Preservación del layout de escritorio

**User Story:** Como usuario de escritorio, quiero que el diseño original no cambie, para seguir usando la aplicación exactamente como antes.

#### Criterios de Aceptación

1. WHILE el viewport es >768px, THE Sistema SHALL mantener el sidebar visible con su ancho original de 250px y posición fija.
2. WHILE el viewport es >768px, THE Sistema SHALL mantener el `margin-left` original del área de contenido sin modificación.
3. THE Sistema SHALL no introducir cambios en los controladores Java, entidades, ni plantillas Thymeleaf más allá de los bloques CSS, HTML y JS descritos en el diseño.

---

### Requisito 8: Compatibilidad y rendimiento

**User Story:** Como usuario, quiero que la animación del sidebar sea fluida y no degrade el rendimiento del navegador, para tener una experiencia ágil en dispositivos móviles.

#### Criterios de Aceptación

1. THE Sistema SHALL implementar la animación del sidebar usando `transform: translateX` para aprovechar la aceleración por GPU y evitar reflow.
2. THE Sistema SHALL implementar el Overlay usando `display: none` / `display: block` para evitar que elementos ocultos reciban eventos de puntero.
3. THE Sistema SHALL no añadir ninguna dependencia externa nueva (librerías JS o CSS adicionales).
