/**
 * Sistema de notificaciones con campana — SIA
 * Uso: siaNotif.init({ tipo, id })
 *   tipo: 'aprendiz' | 'instructor' | 'admin' | 'seguridad'
 *   id:   ficha (aprendiz), instructorId (instructor), adminId (admin), cualquier string (seguridad)
 */
(function () {

  // ── Inyectar HTML + CSS ────────────────────────────────────────────────────
  function inyectarUI() {
    if (document.getElementById('sia-campana')) return;

    const css = `
      #sia-campana {
        position: fixed; top: 18px; right: 24px; z-index: 1500;
        cursor: pointer; display: flex; align-items: center; justify-content: center;
        width: 46px; height: 46px; border-radius: 50%;
        background: linear-gradient(135deg, #006B2D, #008D4D);
        box-shadow: 0 3px 14px rgba(0,107,45,0.45);
        transition: transform 0.2s, box-shadow 0.2s;
        border: none; outline: none;
      }
      #sia-campana:hover { transform: scale(1.1); box-shadow: 0 5px 20px rgba(0,107,45,0.6); }
      #sia-campana i { color: #fff; font-size: 19px; pointer-events: none; }
      #sia-badge {
        position: absolute; top: -5px; right: -5px;
        background: #ef4444; color: #fff;
        font-size: 10px; font-weight: 700;
        min-width: 19px; height: 19px; border-radius: 10px;
        display: none; align-items: center; justify-content: center;
        padding: 0 4px; border: 2px solid #fff;
        pointer-events: none;
      }
      #sia-campana.sia-shake { animation: siaBell 0.5s ease 3; }
      @keyframes siaBell {
        0%,100% { transform: rotate(0) scale(1); }
        20%     { transform: rotate(-18deg) scale(1.1); }
        40%     { transform: rotate(18deg) scale(1.1); }
        60%     { transform: rotate(-10deg); }
        80%     { transform: rotate(10deg); }
      }

      #sia-overlay {
        display: none; position: fixed; inset: 0;
        background: rgba(0,0,0,0.35); z-index: 1600;
        align-items: flex-start; justify-content: flex-end;
        padding: 74px 24px 0;
      }
      #sia-overlay.sia-open { display: flex; }

      #sia-panel {
        background: #fff; border-radius: 18px; width: 360px; max-width: 96vw;
        box-shadow: 0 10px 50px rgba(0,0,0,0.22);
        overflow: hidden;
        animation: siaDown 0.22s ease;
        display: flex; flex-direction: column; max-height: 520px;
      }
      @keyframes siaDown {
        from { opacity: 0; transform: translateY(-14px); }
        to   { opacity: 1; transform: translateY(0); }
      }

      .sia-header {
        background: linear-gradient(135deg, #006B2D, #008D4D);
        color: #fff; padding: 14px 18px;
        display: flex; align-items: center; justify-content: space-between;
        flex-shrink: 0;
      }
      .sia-header h3 { margin: 0; font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
      .sia-header-btns { display: flex; gap: 8px; }
      .sia-header-btns button {
        background: rgba(255,255,255,0.18); border: none; color: #fff;
        font-size: 12px; padding: 4px 10px; border-radius: 6px; cursor: pointer;
        font-family: inherit; transition: background 0.2s; width: auto; margin: 0;
      }
      .sia-header-btns button:hover { background: rgba(255,255,255,0.32); }

      .sia-list { overflow-y: auto; flex: 1; padding: 4px 0; }

      .sia-item {
        padding: 11px 16px; border-bottom: 1px solid #f3f4f6;
        display: flex; gap: 11px; align-items: flex-start;
        transition: background 0.15s; cursor: default;
      }
      .sia-item:hover { background: #f9fafb; }
      .sia-item:last-child { border-bottom: none; }
      .sia-item.sia-unread { background: #f0fdf4; }
      .sia-item.sia-unread:hover { background: #dcfce7; }

      .sia-ico {
        width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
        display: flex; align-items: center; justify-content: center; font-size: 15px;
      }
      .sia-ico.verde   { background: #dcfce7; color: #166534; }
      .sia-ico.azul    { background: #dbeafe; color: #1e40af; }
      .sia-ico.naranja { background: #ffedd5; color: #9a3412; }
      .sia-ico.rojo    { background: #fee2e2; color: #991b1b; }
      .sia-ico.morado  { background: #ede9fe; color: #6d28d9; }
      .sia-ico.gris    { background: #f3f4f6; color: #6b7280; }

      .sia-txt { flex: 1; min-width: 0; }
      .sia-txt strong { display: block; font-size: 13px; color: #111; margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .sia-txt span   { font-size: 12px; color: #555; line-height: 1.45; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
      .sia-txt small  { display: block; font-size: 11px; color: #aaa; margin-top: 4px; }

      .sia-empty {
        text-align: center; padding: 36px 18px; color: #bbb; font-size: 13px;
      }
      .sia-empty i { font-size: 34px; display: block; margin-bottom: 10px; color: #e5e7eb; }

      .sia-footer {
        padding: 9px 16px; border-top: 1px solid #f3f4f6;
        display: flex; justify-content: space-between; align-items: center;
        flex-shrink: 0;
      }
      .sia-footer small { color: #aaa; font-size: 11px; }
      .sia-btn-clear {
        background: transparent; border: 1px solid #e5e7eb; color: #6b7280;
        font-size: 12px; padding: 4px 12px; border-radius: 6px; cursor: pointer;
        font-family: inherit; transition: all 0.2s;
      }
      .sia-btn-clear:hover { background: #fef2f2; border-color: #fca5a5; color: #dc2626; }

      /* Dark mode */
      [data-theme="dark"] #sia-panel             { background: #161b27; }
      [data-theme="dark"] .sia-item              { border-color: #2d3748; color: #e2e8f0; }
      [data-theme="dark"] .sia-item:hover        { background: #1e2535; }
      [data-theme="dark"] .sia-item.sia-unread   { background: #0f2318; }
      [data-theme="dark"] .sia-txt strong        { color: #e6edf3; }
      [data-theme="dark"] .sia-txt span          { color: #94a3b8; }
      [data-theme="dark"] .sia-footer            { border-color: #2d3748; }
      [data-theme="dark"] .sia-btn-clear         { border-color: #2d3748; color: #94a3b8; }
    `;

    const style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);

    document.body.insertAdjacentHTML('beforeend', `
      <button id="sia-campana" title="Notificaciones" aria-label="Ver notificaciones">
        <i class="fas fa-bell"></i>
        <span id="sia-badge"></span>
      </button>

      <div id="sia-overlay">
        <div id="sia-panel" role="dialog" aria-modal="true" aria-label="Notificaciones">
          <div class="sia-header">
            <h3><i class="fas fa-bell"></i> Notificaciones</h3>
            <div class="sia-header-btns">
              <button onclick="siaNotif.marcarTodasLeidas()"><i class="fas fa-check-double"></i> Leídas</button>
              <button onclick="siaNotif.cerrar()"><i class="fas fa-times"></i></button>
            </div>
          </div>
          <div class="sia-list" id="sia-list"></div>
          <div class="sia-footer">
            <small id="sia-footer-count">0 notificaciones</small>
            <button class="sia-btn-clear" onclick="siaNotif.limpiar()"><i class="fas fa-trash-alt"></i> Limpiar todo</button>
          </div>
        </div>
      </div>
    `);

    document.getElementById('sia-campana').addEventListener('click', () => siaNotif.toggle());
    document.getElementById('sia-overlay').addEventListener('click', function (e) {
      if (e.target === this) siaNotif.cerrar();
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') siaNotif.cerrar(); });

    renderizar();
  }

  // ── Estado ─────────────────────────────────────────────────────────────────
  const _notis = [];
  let _ws = null;
  let _wsOpts = null;

  // ── Mapa de tipos a icono + color ──────────────────────────────────────────
  const TIPOS = {
    'nuevo_mensaje':     { icon: 'fa-comment-dots',      clase: 'azul',    label: 'Nuevo mensaje'         },
    'nueva_tarea':       { icon: 'fa-clipboard-list',    clase: 'morado',  label: 'Nueva tarea'           },
    'tarea_creada':      { icon: 'fa-clipboard-list',    clase: 'morado',  label: 'Tarea creada'          },
    'tarea_entregada':   { icon: 'fa-paper-plane',       clase: 'verde',   label: 'Tarea entregada'       },
    'estado_solicitud':  { icon: 'fa-door-open',         clase: 'naranja', label: 'Solicitud respondida'  },
    'solicitud_ambiente':{ icon: 'fa-building',          clase: 'naranja', label: 'Solicitud de ambiente' },
    'usuario_registro':  { icon: 'fa-user-plus',         clase: 'verde',   label: 'Nuevo usuario'         },
    'incapacidad':       { icon: 'fa-file-medical',      clase: 'naranja', label: 'Incapacidad'           },
    'inasistencia':      { icon: 'fa-calendar-times',    clase: 'rojo',    label: 'Inasistencia'          },
    'asistencia':        { icon: 'fa-calendar-check',    clase: 'azul',    label: 'Asistencia'            },
    'certificado':       { icon: 'fa-certificate',       clase: 'verde',   label: 'Certificado'           },
    'acceso':            { icon: 'fa-sign-in-alt',       clase: 'azul',    label: 'Acceso'                },
    'error':             { icon: 'fa-exclamation-circle',clase: 'rojo',    label: 'Error'                 },
    'general':           { icon: 'fa-info-circle',       clase: 'gris',    label: 'Información'           },
  };

  function iconoPorTipo(tipo) {
    return TIPOS[tipo] || { icon: 'fa-bell', clase: 'verde', label: 'Notificación' };
  }

  // ── Tiempo relativo ────────────────────────────────────────────────────────
  function tiempoRelativo(ts) {
    const diff = Math.floor((Date.now() - ts) / 1000);
    if (diff < 60)  return 'ahora mismo';
    if (diff < 3600) return `hace ${Math.floor(diff/60)} min`;
    if (diff < 86400) return `hace ${Math.floor(diff/3600)} h`;
    return new Date(ts).toLocaleDateString('es-CO', { day:'2-digit', month:'short' });
  }

  // ── Agregar notificación ───────────────────────────────────────────────────
  function agregar(noti) {
    _notis.unshift({ ...noti, _ts: Date.now(), _leida: false });
    if (_notis.length > 60) _notis.pop();

    // Sonido (con fallback silencioso)
    try {
      const a = document.getElementById('sonidoNotificacion');
      if (a) { a.currentTime = 0; a.play().catch(() => {}); }
      else {
        const audio = new Audio('/sounds/notificacion.mp3');
        audio.volume = 0.55;
        audio.play().catch(() => {});
      }
    } catch (_) {}

    // Animación campana
    const campana = document.getElementById('sia-campana');
    if (campana) {
      campana.classList.remove('sia-shake');
      void campana.offsetWidth;
      campana.classList.add('sia-shake');
      setTimeout(() => campana.classList.remove('sia-shake'), 1600);
    }

    actualizarBadge();

    // Si el modal está abierto, re-renderizar en tiempo real
    const overlay = document.getElementById('sia-overlay');
    if (overlay && overlay.classList.contains('sia-open')) {
      renderizar();
    }
  }

  // ── Badge ──────────────────────────────────────────────────────────────────
  function actualizarBadge() {
    const badge = document.getElementById('sia-badge');
    if (!badge) return;
    const n = _notis.filter(x => !x._leida).length;
    badge.textContent = n > 9 ? '9+' : n;
    badge.style.display = n > 0 ? 'flex' : 'none';
  }

  // ── Renderizar lista ───────────────────────────────────────────────────────
  function renderizar() {
    const list = document.getElementById('sia-list');
    const count = document.getElementById('sia-footer-count');
    if (!list) return;

    if (count) count.textContent = `${_notis.length} notificación${_notis.length !== 1 ? 'es' : ''}`;

    if (_notis.length === 0) {
      list.innerHTML = `<div class="sia-empty"><i class="fas fa-bell-slash"></i>Sin notificaciones nuevas</div>`;
      return;
    }

    list.innerHTML = _notis.map(n => {
      const { icon, clase } = iconoPorTipo(n.tipo);
      const tiempo = tiempoRelativo(n._ts);
      return `
        <div class="sia-item${n._leida ? '' : ' sia-unread'}">
          <div class="sia-ico ${clase}"><i class="fas ${icon}"></i></div>
          <div class="sia-txt">
            <strong>${escHtml(n.titulo || 'Notificación')}</strong>
            <span>${escHtml(n.mensaje || '')}</span>
            <small>${tiempo}${n.remitente ? ' · ' + escHtml(n.remitente) : ''}</small>
          </div>
        </div>`;
    }).join('');
  }

  function escHtml(s) {
    return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ── Cargar notificaciones offline desde el servidor ───────────────────────
  function cargarPendientes() {
    fetch('/api/notificaciones/pendientes', { credentials: 'same-origin' })
      .then(r => { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(lista => {
        if (!Array.isArray(lista) || lista.length === 0) return;
        // Insertar al final (son las más viejas, las del WS van al frente)
        lista.forEach(n => {
          _notis.push({ ...n, _ts: Date.now() - 60000, _leida: false });
        });
        if (_notis.length > 60) _notis.length = 60;
        actualizarBadge();
        renderizar();
      })
      .catch(() => {}); // Silencioso — puede que el usuario no tenga sesión aún
  }

  // ── WebSocket ──────────────────────────────────────────────────────────────
  function conectar(tipo, id) {
    if (!tipo || !id) return;
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${proto}://${location.host}/notificaciones/${tipo}/${encodeURIComponent(id)}`;

    _ws = new WebSocket(url);

    _ws.onopen = () => {
      console.info(`🔔 Notificaciones WS conectado [${tipo}/${id}]`);
    };

    _ws.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data);
        // Evitar procesar mensajes de chat que llegan por el handler equivocado
        if (data.tipo === 'ping') return;
        agregar(data);
      } catch (_) {}
    };

    _ws.onerror = () => {};

    _ws.onclose = () => {
      console.info('🔔 WS notificaciones cerrado, reconectando en 5s…');
      setTimeout(() => conectar(tipo, id), 5000);
    };
  }

  // ── API pública ────────────────────────────────────────────────────────────
  window.siaNotif = {
    init(opts) {
      inyectarUI();
      // 1. Cargar notificaciones persistidas (offline) antes de abrir WS
      cargarPendientes();
      // 2. Abrir WebSocket para notificaciones en tiempo real
      if (opts && opts.tipo && opts.id) {
        _wsOpts = opts;
        conectar(opts.tipo, String(opts.id));
      }
    },

    toggle() {
      const overlay = document.getElementById('sia-overlay');
      if (!overlay) return;
      if (overlay.classList.contains('sia-open')) {
        this.cerrar();
      } else {
        overlay.classList.add('sia-open');
        this.marcarTodasLeidas();
        renderizar();
      }
    },

    cerrar() {
      const overlay = document.getElementById('sia-overlay');
      if (overlay) overlay.classList.remove('sia-open');
    },

    marcarTodasLeidas() {
      _notis.forEach(n => n._leida = true);
      actualizarBadge();
      // Persistir en servidor (silencioso)
      fetch('/api/notificaciones/leidas', { method: 'POST', credentials: 'same-origin' }).catch(() => {});
    },

    limpiar() {
      _notis.length = 0;
      actualizarBadge();
      renderizar();
    },

    /** Agregar notificación desde código externo */
    agregar,

    /** Acceso al estado de notificaciones (solo lectura) */
    get count() { return _notis.filter(n => !n._leida).length; }
  };

})();
