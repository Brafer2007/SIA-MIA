// ===== SISTEMA DE TEMAS CLARO / OSCURO =====
(function () {
  const STORAGE_KEY = 'sia-theme';

  // Aplicar tema guardado inmediatamente (antes del render)
  const saved = localStorage.getItem(STORAGE_KEY) || 'light';
  document.documentElement.setAttribute('data-theme', saved);

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(STORAGE_KEY, theme);

    // Actualizar todos los botones de toggle en la página
    document.querySelectorAll('.theme-toggle-btn').forEach(btn => {
      const icon = btn.querySelector('.theme-icon');
      const label = btn.querySelector('.theme-label');
      if (theme === 'dark') {
        if (icon)  icon.className = 'theme-icon fas fa-sun';
        if (label) label.textContent = 'Tema Claro';
      } else {
        if (icon)  icon.className = 'theme-icon fas fa-moon';
        if (label) label.textContent = 'Tema Oscuro';
      }
    });
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    applyTheme(current === 'dark' ? 'light' : 'dark');
  }

  // Exponer globalmente
  window.toggleTheme = toggleTheme;

  // Aplicar al cargar DOM
  document.addEventListener('DOMContentLoaded', function () {
    applyTheme(localStorage.getItem(STORAGE_KEY) || 'light');
  });
})();
