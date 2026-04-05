/**
 * Sistema de notificaciones con campana — SIA
 * Uso: llamar initNotificaciones({ tipo, id }) desde cada dashboard
 * tipo: 'aprendiz' | 'instructor' | 'seguridad'
 * id:   ficha (aprendiz), instructorId (instructor), cualquier string (seguridad)
 */
(function () {

  // ── Inyectar HTML de campana + modal si no existe ──────────────────────────
  function inyectarUI() {
    if (document.getElementById('sia-campana')) return;

    const css = `
      #sia-campana {
        position: fixed; top: 18px; right: 24px; z-index: 1500;
        cursor: pointer; display: flex; align-items: center; justify-content: center;
        width: 44px; height: 44px; border-radius: 50%;
        background: rgba(0,107,45,0.9);
        box-shadow: 0 2px 12px rgba(0,107,45,0.4);
        transition: transform 0.2s, box-shadow 0.2s;
      }
      #sia-campana:hover { transform: scale(1.1); box-shadow: 0 4px 18px rgba(0,107,45,0.55); }
      #sia-campana i { color: #fff; font-size: 18px; }
      #sia-campana .sia-badge {
        position: absolute; top: -4px; right: -4px;
        background: #ef4444; color: #fff;
        font-size: 10px; font-weight: 700;
        min-width: 18px; height: 18px; border-radius: 9px;
        display: none; align-items: center; justify-content: center;
        padding: 0 4px; border: 2px solid #fff;
      }
      #sia-campana.shake { animation: siaBell 0.5s ease 3; }
      @keyframes siaBell {
        0%,100% { transform: rotate(0); }
        20%      { transform: rotate(-15deg); }
        40%      { transform: rotate(15deg); }
        60%      { transform: rotate(-10deg); }
        80%      { transform: rotate(10deg); }
      }

      #sia-modal-overlay {
        display: none; position: fixed; inset: 0;
        background: rgba(0,0,0,0.45); z-index: 1600;
        align-items: flex-start; justify-content: flex-end;
        padding: 72px 24px 0;
      }
      #sia-modal-overlay.open { display: flex; }

      #sia-modal {
        background: #fff; border-radius: 16px; width: 340px; max-width: 95vw;
        box-shadow: 0 8px 40px rgba(0,0,0,0.2);
        overflow: hidden;
        animation: siaSlideDown 0.25s ease;
      }
      @keyframes siaSlideDown {
        from { opacity: 0; transform: translateY(-12px); }
        to   { opacity: 1; transform: translateY(0); }
      }
      .sia-modal-header {
        background: #006B2D; color: #fff;
        padding: 14px 18px; display: flex;
        align-items: center; justify-content: space-between;
      }
      .sia-modal-header h3 { margin: 0; font-size: 15px; font-weight: 600; }
      .sia-modal-header button {
        background: transparent; border: none; color: #fff;
        font-size: 18px; cursor: pointer; padding: 0; line-height: 1;
        width: auto; margin: 0;
      }
      .sia-modal-header button:hover { opacity: 0.75; }
      .sia-modal-body { max-height: 380px; overflow-y: auto; padding: 8px 0; }
      .sia-noti-item {
        padding: 12px 18px; border-bottom: 1px solid #f0f0f0;
        display: flex; gap: 12px; align-items: flex-start;
        transition: background 0.15s;
      }
      .sia-noti-item:hover { background: #f9fafb; }
      .sia-noti-item:last-child { border-bottom: none; }
      .sia-noti-icon {
        width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
        display: flex; align-items: center; justify-content: center;
        font-size: 16px;
      }
      .sia-noti-icon.verde  { background: #dcfce7; color: #166534; }
      .sia-noti-icon.azul   { background: #dbeafe; color: #1e40af; }
      .sia-noti-icon.naranja{ background: #ffedd5; color: #9a3412; }
      .sia-noti-icon.rojo   { background: #fee2e2; color: #991b1b; }
      .sia-noti-text { flex: 1; }
      .sia-noti-text strong { display: block; font-size: 13px; color: #111; margin-bottom: 2px; }
      .sia-noti-text span   { font-size: 12px; color: #666; line-height: 1.4; }
      .sia-noti-text small  { display: block; font-size: 11px; color: #aaa; margin-top: 4px; }
      .sia-empty {
        text-align: center; padding: 32px 18px; color: #aaa; font-size: 13px;
      }
      .sia-empty i { font-size: 32px; display: block; margin-bottom: 8px; color: #ddd; }
      .sia-modal-footer {
        padding: 10px 18px; border-top: 1px solid #f0f0f0;
        display: flex; justify-content: flex-end;
      }
      .sia-btn-limpiar {
        background: transparent; border: 1px solid #ddd; color: #666;
        font-size: 12px; padding: 5px 12px; border-radius: 6px; cursor: pointer;
        font-family: inherit;
      }
      .sia-btn-limpiar:hover { background: #f9fafb; }

      /* Tema oscuro */
      [data-theme="dark"] #sia-modal { background: #161b27; }
      [data-theme="dark"] .sia-noti-item { border-color: #2d3748; }
      [data-theme="dark"] .sia-noti-item:hover { background: #1e2535; }
      [data-theme="dark"] .sia-noti-text strong { color: #e6edf3; }
      [data-theme="dark"] .sia-noti-text span   { color: #94a3b8; }
      [data-theme="dark"] .sia-modal-footer { border-color: #2d3748; }
      [data-theme="dark"] .sia-btn-limpiar { border-color: #2d3748; color: #94a3b8; }
      [data-theme="dark"] .sia-btn-limpiar:hover { background: #1e2535; }
    `;

    const style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);

    document.body.insertAdjacentHTML('beforeend', `
      <div id="sia-campana" title="Notificaciones" role="button" tabindex="0" aria-label="Ver notificaciones">
        <i class="fas fa-bell"></i>
        <span class="sia-badge" id="sia-badge"></span>
      </div>

      <div id="sia-modal-overlay">
        <div id="sia-modal" role="dialog" aria-modal="true" aria-label="Notificaciones">
          <div class="sia-modal-header">
            <h3><i class="fas fa-bell" style="margin-right:8px;"></i>Notificaciones</h3>
            <button onclick="siaNotif.cerrar()" aria-label="Cerrar">&times;</button>
          </div>
          <div class="sia-modal-body" id="sia-modal-body">
            <div class="sia-empty">
              <i class="fas fa-bell-slash"></i>
              Sin notificaciones nuevas
            </div>
          </div>
          <div class="sia-modal-footer">
            <button class="sia-btn-limpiar" onclick="siaNotif.limpiar()">
              <i class="fas fa-trash-alt"></i> Limpiar todo
            </button>
          </div>
        </div>
      </div>
    `);

    // Eventos
    document.getElementById('sia-campana').addEventListener('click', () => siaNotif.toggle());
    document.getElementById('sia-modal-overlay').addEventListener('click', function (e) {
      if (e.target === this) siaNotif.cerrar();
    });
  }

  // ── Estado ─────────────────────────────────────────────────────────────────
  const _notis = [];
  let _ws = null;

  // ── Iconos por tipo ────────────────────────────────────────────────────────
  function iconoPorTipo(tipo) {
    const mapa = {
      'nuevo_mensaje':    { icon: 'fa-comment-dots',   clase: 'azul'    },
      'tarea_creada':     { icon: 'fa-clipboard-list', clase: 'verde'   },
      'tarea_entregada':  { icon: 'fa-paper-plane',    clase: 'naranja' },
      'estado_solicitud': { icon: 'fa-door-open',      clase: 'verde'   },
      'usuario_registro': { icon: 'fa-user-plus',      clase: 'verde'   },
      'incapacidad':      { icon: 'fa-file-medical',   clase: 'naranja' },
      'asistencia':       { icon: 'fa-calendar-check', clase: 'azul'    },
      'error':            { icon: 'fa-exclamation-circle', clase: 'rojo' },
    };
    return mapa[tipo] || { icon: 'fa-bell', clase: 'verde' };
  }

  // ── Agregar notificación ───────────────────────────────────────────────────
  function agregar(noti) {
    const ahora = new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    _notis.unshift({ ...noti, hora: ahora, leida: false });
    if (_notis.length > 50) _notis.pop();

    // Sonido
    try {
      const audio = new Audio('/sounds/notificacion.mp3');
      audio.volume = 0.6;
      audio.play().catch(() => {});
    } catch (e) {}

    // Animación campana
    const campana = document.getElementById('sia-campana');
    if (campana) {
      campana.classList.remove('shake');
      void campana.offsetWidth;
      campana.classList.add('shake');
      setTimeout(() => campana.classList.remove('shake'), 1600);
    }

    actualizarBadge();
    renderizar();
  }

  // ── Badge ──────────────────────────────────────────────────────────────────
  function actualizarBadge() {
    const badge = document.getElementById('sia-badge');
    if (!badge) return;
    const noLeidas = _notis.filter(n => !n.leida).length;
    badge.textContent = noLeidas > 9 ? '9+' : noLeidas;
    badge.style.display = noLeidas > 0 ? 'flex' : 'none';
  }

  // ── Renderizar lista ───────────────────────────────────────────────────────
  function renderizar() {
    const body = document.getElementById('sia-modal-body');
    if (!body) return;

    if (_notis.length === 0) {
      body.innerHTML = `<div class="sia-empty"><i class="fas fa-bell-slash"></i>Sin notificaciones nuevas</div>`;
      return;
    }

    body.innerHTML = _notis.map(n => {
      const { icon, clase } = iconoPorTipo(n.tipo);
      return `
        <div class="sia-noti-item">
          <div class="sia-noti-icon ${clase}"><i class="fas ${icon}"></i></div>
          <div class="sia-noti-text">
            <strong>${n.titulo || 'Notificación'}</strong>
            <span>${n.mensaje || ''}</span>
            <small>${n.hora}</small>
          </div>
        </div>`;
    }).join('');
  }

  // ── WebSocket ──────────────────────────────────────────────────────────────
  function conectar(tipo, id) {
    if (!tipo || !id) return;
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${proto}://${location.host}/notificaciones/${tipo}/${encodeURIComponent(id)}`;

    _ws = new WebSocket(url);

    _ws.onopen = () => console.info(`🔔 Notificaciones WS conectado [${tipo}/${id}]`);

    _ws.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data);
        agregar(data);
      } catch (err) {
        console.warn('Notificación no parseable:', e.data);
      }
    };

    _ws.onerror = (err) => console.error('WS notificaciones error:', err);

    _ws.onclose = () => {
      console.info('WS notificaciones cerrado, reconectando en 5s...');
      setTimeout(() => conectar(tipo, id), 5000);
    };
  }

  // ── API pública ────────────────────────────────────────────────────────────
  window.siaNotif = {
    init(opts) {
      inyectarUI();
      if (opts && opts.tipo && opts.id) {
        conectar(opts.tipo, String(opts.id));
      }
    },
    toggle() {
      const overlay = document.getElementById('sia-modal-overlay');
      if (!overlay) return;
      const abierto = overlay.classList.contains('open');
      if (abierto) {
        this.cerrar();
      } else {
        overlay.classList.add('open');
        // Marcar como leídas al abrir
        _notis.forEach(n => n.leida = true);
        actualizarBadge();
        renderizar();
      }
    },
    cerrar() {
      const overlay = document.getElementById('sia-modal-overlay');
      if (overlay) overlay.classList.remove('open');
    },
    limpiar() {
      _notis.length = 0;
      actualizarBadge();
      renderizar();
    },
    agregar
  };

})();
