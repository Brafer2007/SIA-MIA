/**
 * perfil-widget.js — Widget de cambio de contraseña y foto de perfil.
 * Uso: perfilWidget.init() desde cualquier dashboard.
 * Inyecta automáticamente el modal y los botones donde encuentre
 * el elemento con id="perfil-widget-anchor".
 */
(function () {

  function inyectarHTML() {
    const anchor = document.getElementById('perfil-widget-anchor');
    if (!anchor) return;

    anchor.insertAdjacentHTML('beforeend', `
      <!-- ── Foto de perfil ── -->
      <div style="display:flex; align-items:center; gap:20px; margin-bottom:20px; flex-wrap:wrap;">
        <div style="position:relative; width:80px; height:80px;">
          <img id="pw-foto-preview"
               src="/img/avatar-default.svg"
               onerror="this.src='/img/avatar-default.svg'"
               style="width:80px;height:80px;border-radius:50%;object-fit:cover;
                      border:3px solid #5b21b6;box-shadow:0 2px 8px rgba(91,33,182,0.3);"
               alt="Foto de perfil">
          <label for="pw-foto-input"
                 title="Cambiar foto"
                 style="position:absolute;bottom:0;right:0;background:#5b21b6;color:#fff;
                        border-radius:50%;width:24px;height:24px;display:flex;align-items:center;
                        justify-content:center;cursor:pointer;font-size:11px;">
            <i class="fas fa-camera"></i>
          </label>
          <input type="file" id="pw-foto-input" accept="image/*"
                 style="display:none;" onchange="perfilWidget.subirFoto(this)">
        </div>
        <div>
          <p id="pw-foto-msg" style="font-size:12px;color:#888;margin:0;"></p>
          <button id="pw-btn-eliminar-foto" onclick="perfilWidget.eliminarFoto()"
                  style="display:none;margin-top:6px;background:transparent;color:#dc2626;
                         border:1px solid #fca5a5;padding:4px 12px;border-radius:6px;
                         font-size:12px;cursor:pointer;">
            <i class="fas fa-trash-alt"></i> Quitar foto
          </button>
        </div>
      </div>

      <!-- ── Cambiar contraseña ── -->
      <details style="margin-top:8px;">
        <summary style="cursor:pointer;font-weight:600;color:#5b21b6;font-size:14px;
                        list-style:none;display:flex;align-items:center;gap:8px;">
          <i class="fas fa-key"></i> Cambiar contraseña
          <i class="fas fa-chevron-down" style="font-size:11px;margin-left:auto;"></i>
        </summary>
        <div style="margin-top:14px;padding:16px;background:#f8fffe;border-radius:10px;
                    border:1px solid #d1fae5;">
          <div id="pw-pass-msg" style="display:none;padding:8px 14px;border-radius:7px;
                                        font-size:13px;margin-bottom:12px;"></div>
          <div style="margin-bottom:12px;">
            <label style="display:block;font-size:11px;font-weight:600;color:#555;
                           text-transform:uppercase;letter-spacing:.5px;margin-bottom:5px;">
              Contraseña actual
            </label>
            <input type="password" id="pw-actual"
                   placeholder="Tu contraseña actual"
                   style="width:100%;padding:9px 12px;border:1.5px solid #d1fae5;
                          border-radius:8px;font-size:13px;outline:none;
                          font-family:inherit;background:#fff;">
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:14px;">
            <div>
              <label style="display:block;font-size:11px;font-weight:600;color:#555;
                             text-transform:uppercase;letter-spacing:.5px;margin-bottom:5px;">
                Nueva contraseña
              </label>
              <input type="password" id="pw-nueva"
                     placeholder="Mínimo 6 caracteres"
                     style="width:100%;padding:9px 12px;border:1.5px solid #d1fae5;
                            border-radius:8px;font-size:13px;outline:none;
                            font-family:inherit;background:#fff;">
            </div>
            <div>
              <label style="display:block;font-size:11px;font-weight:600;color:#555;
                             text-transform:uppercase;letter-spacing:.5px;margin-bottom:5px;">
                Confirmar
              </label>
              <input type="password" id="pw-confirm"
                     placeholder="Repite la nueva"
                     style="width:100%;padding:9px 12px;border:1.5px solid #d1fae5;
                            border-radius:8px;font-size:13px;outline:none;
                            font-family:inherit;background:#fff;">
            </div>
          </div>
          <button onclick="perfilWidget.cambiarPassword()"
                  id="pw-btn-cambiar"
                  style="background:linear-gradient(135deg,#5b21b6,#7c3aed);color:#fff;
                         border:none;padding:10px 22px;border-radius:8px;font-weight:600;
                         font-size:13px;cursor:pointer;transition:opacity .2s;">
            <i class="fas fa-lock"></i> Actualizar contraseña
          </button>
        </div>
      </details>
    `);

    // Cargar foto actual si existe
    fetch('/api/perfil/foto-url', { credentials: 'same-origin' })
      .then(r => r.ok ? r.json() : null)
      .then(data => {
        if (data && data.url) {
          document.getElementById('pw-foto-preview').src = data.url;
          document.getElementById('pw-btn-eliminar-foto').style.display = 'inline-block';
        }
      })
      .catch(() => {});
  }

  window.perfilWidget = {
    init() { inyectarHTML(); },

    subirFoto(input) {
      const file = input.files[0];
      if (!file) return;
      if (file.size > 3 * 1024 * 1024) {
        this._msg('pw-foto-msg', 'La imagen no puede superar 3 MB.', 'error');
        return;
      }
      const fd = new FormData();
      fd.append('foto', file);
      fetch('/api/perfil/foto', { method: 'POST', body: fd, credentials: 'same-origin' })
        .then(r => r.json())
        .then(data => {
          if (data.url) {
            document.getElementById('pw-foto-preview').src = data.url + '?t=' + Date.now();
            document.getElementById('pw-btn-eliminar-foto').style.display = 'inline-block';
            this._msg('pw-foto-msg', '✓ Foto actualizada', 'ok');
          } else {
            this._msg('pw-foto-msg', data.error || 'Error al subir.', 'error');
          }
        })
        .catch(() => this._msg('pw-foto-msg', 'Error de conexión.', 'error'));
    },

    eliminarFoto() {
      if (!confirm('¿Eliminar tu foto de perfil?')) return;
      fetch('/api/perfil/foto', { method: 'DELETE', credentials: 'same-origin' })
        .then(r => r.json())
        .then(() => {
          document.getElementById('pw-foto-preview').src = '/img/avatar-default.svg';
          document.getElementById('pw-btn-eliminar-foto').style.display = 'none';
          this._msg('pw-foto-msg', 'Foto eliminada.', 'ok');
        });
    },

    cambiarPassword() {
      const actual   = document.getElementById('pw-actual').value;
      const nueva    = document.getElementById('pw-nueva').value;
      const confirm  = document.getElementById('pw-confirm').value;
      if (!actual || !nueva || !confirm) {
        this._msg('pw-pass-msg', 'Completa todos los campos.', 'error'); return;
      }
      if (nueva.length < 6) {
        this._msg('pw-pass-msg', 'La nueva contraseña debe tener al menos 6 caracteres.', 'error'); return;
      }
      if (nueva !== confirm) {
        this._msg('pw-pass-msg', 'Las contraseñas nuevas no coinciden.', 'error'); return;
      }
      const btn = document.getElementById('pw-btn-cambiar');
      btn.disabled = true; btn.textContent = 'Actualizando...';
      const fd = new FormData();
      fd.append('passwordActual', actual);
      fd.append('passwordNueva', nueva);
      fd.append('passwordConfirm', confirm);
      fetch('/api/perfil/cambiar-password', { method: 'POST', body: fd, credentials: 'same-origin' })
        .then(r => r.json())
        .then(data => {
          if (data.mensaje) {
            this._msg('pw-pass-msg', '✓ ' + data.mensaje, 'ok');
            document.getElementById('pw-actual').value = '';
            document.getElementById('pw-nueva').value  = '';
            document.getElementById('pw-confirm').value = '';
          } else {
            this._msg('pw-pass-msg', data.error || 'Error al actualizar.', 'error');
          }
        })
        .catch(() => this._msg('pw-pass-msg', 'Error de conexión.', 'error'))
        .finally(() => { btn.disabled = false; btn.innerHTML = '<i class="fas fa-lock"></i> Actualizar contraseña'; });
    },

    _msg(id, texto, tipo) {
      const el = document.getElementById(id);
      if (!el) return;
      el.textContent = texto;
      el.style.display = 'block';
      el.style.background = tipo === 'ok' ? '#d4edda' : '#f8d7da';
      el.style.color      = tipo === 'ok' ? '#155724' : '#721c24';
      el.style.border     = '1px solid ' + (tipo === 'ok' ? '#c3e6cb' : '#f5c6cb');
      setTimeout(() => { el.style.display = 'none'; }, 5000);
    }
  };

})();
