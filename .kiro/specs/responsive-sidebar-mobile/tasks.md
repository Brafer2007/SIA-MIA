# Plan de Implementación: responsive-sidebar-mobile

## Visión General

Aplicar soporte responsive (sidebar oculto + botón hamburguesa + overlay) a los 11 archivos HTML del proyecto SIA. Cada archivo recibe el mismo bloque CSS, dos elementos HTML y dos funciones JS. El archivo `dashboardAdministrador.html` requiere reemplazar un media query existente. No hay cambios en el backend.

## Tareas

- [x] 1. Modificar dashboardAdministrador.html
  - Reemplazar el bloque `@media (max-width: 720px)` existente por el CSS responsive unificado a 768px
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 7.1, 7.2, 8.1, 8.2, 8.3_

  - [ ]* 1.1 Escribir test de propiedad: bloque CSS correcto
    - **Property 1: Bloque CSS correcto en todos los archivos**
    - Verificar que el HTML resultante contiene `transform: translateX(-100%)`, `margin-left: 0`, `display: none` para `.hamburger-btn` fuera del media query, `display: block` dentro, `transform: translateX(0)` para `.sidebar.open`, `transition: transform 0.3s ease`, y `display: block` para `.sidebar-overlay.active`
    - **Validates: Requirements 1.1, 1.2, 2.1, 2.2, 3.2, 3.3, 8.1, 8.2**

  - [ ]* 1.2 Escribir test de propiedad: cuatro componentes presentes
    - **Property 4: Los cuatro componentes están presentes en todos los archivos**
    - Verificar que el HTML contiene el CSS responsive, el `<button class="hamburger-btn">` con `aria-label`, el `<div class="sidebar-overlay">`, y las funciones JS
    - **Validates: Requirements 2.3, 5.1, 5.2, 5.3, 5.4**

- [x] 2. Modificar dashboardAprendiz.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 3. Modificar dashboardInstructor.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 4. Modificar dashboardInvitado.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 5. Modificar dashboardSeguridad.html (variante `.main-content`)
  - Añadir el bloque CSS responsive al final del `<style>` existente; la regla del media query debe incluir `.content, .main-content { margin-left: 0 !important; }`
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 6. Checkpoint — Verificar los 5 dashboards principales
  - Asegurarse de que los 5 dashboards modificados compilan sin errores Thymeleaf y que los tests de propiedad pasan. Consultar al usuario si surgen dudas.

- [x] 7. Modificar aprendiz/tareas/lista.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 8. Modificar aprendiz/tareas/detalle.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 9. Modificar instructor/tareas/lista.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 10. Modificar instructor/tareas/entregas.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 11. Modificar instructor/tareas/nueva.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 12. Modificar nuevo.html
  - Añadir el bloque CSS responsive al final del `<style>` existente
  - Insertar `<button class="hamburger-btn">` antes del `<div class="sidebar">`
  - Insertar `<div class="sidebar-overlay">` después del cierre del sidebar
  - Añadir `toggleSidebar()` y `closeSidebar()` al bloque `<script>` existente
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 8.1, 8.2, 8.3_

- [x] 13. Checkpoint final — Verificar los 11 archivos
  - Asegurarse de que todos los archivos modificados compilan sin errores Thymeleaf y que los tests de propiedad pasan. Consultar al usuario si surgen dudas.

  - [ ]* 13.1 Escribir test de propiedad: toggle es involución
    - **Property 2: Toggle del sidebar es una involución**
    - Verificar que llamar a `toggleSidebar()` dos veces consecutivas devuelve el sidebar y el overlay al estado inicial
    - **Validates: Requirements 3.1, 4.2**

  - [ ]* 13.2 Escribir test de propiedad: closeSidebar() es idempotente
    - **Property 3: closeSidebar() es idempotente**
    - Verificar que llamar a `closeSidebar()` una o más veces siempre resulta en sidebar sin clase `open` y overlay sin clase `active`
    - **Validates: Requirements 4.3**

  - [ ]* 13.3 Escribir test de propiedad: no-regresión del layout de escritorio
    - **Property 5: No-regresión del layout de escritorio**
    - Verificar que los estilos fuera del media query no modifican `margin-left` del contenido ni el ancho/visibilidad del sidebar respecto al original
    - **Validates: Requirements 1.3, 7.1, 7.2**

  - [ ]* 13.4 Escribir test de propiedad: sin dependencias externas nuevas
    - **Property 6: Sin dependencias externas nuevas**
    - Verificar que el HTML resultante no contiene `<script src="...">` ni `<link rel="stylesheet" href="...">` que no estuvieran en el archivo original
    - **Validates: Requirements 8.3**

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia requisitos específicos para trazabilidad
- Los checkpoints garantizan validación incremental
- El orden de las tareas sigue la prioridad: dashboards principales primero, luego páginas de tareas
