#!/usr/bin/env python3
import re

filepath = 'src/main/resources/templates/dashboardAdministrador.html'

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

print(f"File length: {len(content)} chars")
print(f"Has _origShowSection: {'_origShowSection' in content}")
print(f"Has cargarReporteAdmin: {'cargarReporteAdmin' in content}")

# Fix 1: The broken chartUsuarios block
# Find the pattern where chartUsuarios chart is not properly closed
# and setInterval is misplaced inside the fetch chain
old1 = """            new Chart(document.getElementById("chartUsuarios"), {
              type: "pie",
              data: { labels: Object.keys(data), datasets: [{ data: Object.values(data), backgroundColor: chartTheme.pieColors }] },
              options: { responsive: true, maintainAspectRatio: false }
            });
    // Polling ligero"""

new1 = """            new Chart(document.getElementById("chartUsuarios"), {
              type: "pie",
              data: { labels: Object.keys(data), datasets: [{ data: Object.values(data), backgroundColor: chartTheme.pieColors }] },
              options: { responsive: true, maintainAspectRatio: false }
            });
          }
        }).catch(()=>{/* silent */});
    });

    // Polling ligero"""

if old1 in content:
    content = content.replace(old1, new1, 1)
    print("Fix 1 applied: chartUsuarios block fixed")
else:
    print("Fix 1 NOT applied - pattern not found")
    # Show context around chartUsuarios
    idx = content.find('chartUsuarios')
    while idx != -1:
        print(f"  Found at {idx}: {repr(content[idx:idx+50])}")
        idx = content.find('chartUsuarios', idx+1)

# Fix 2 & 3: Replace _origShowSection/showSection pattern with addEventListener
# Also removes the orphan });
old2 = """    // Inicializar fecha de hoy al abrir la sección
    const _origShowSection = showSection;
    showSection = function(id) {
      _origShowSection(id);
      if (id === 'accesos') {
        const input = document.getElementById('admin-reporte-fecha');
        if (input && !input.value) {
          input.value = new Date().toISOString().split('T')[0];
          cargarReporteAdmin();
        }
      }
    };
    });"""

new2 = """    // Inicializar fecha de hoy al abrir la sección
    document.addEventListener('click', function(e) {
      const btn = e.target.closest('[onclick]');
      if (btn && btn.getAttribute('onclick') && btn.getAttribute('onclick').includes("'accesos'")) {
        setTimeout(function() {
          const input = document.getElementById('admin-reporte-fecha');
          if (input && !input.value) input.value = new Date().toISOString().split('T')[0];
          cargarReporteAdmin();
        }, 50);
      }
    });"""

if old2 in content:
    content = content.replace(old2, new2, 1)
    print("Fix 2&3 applied: _origShowSection replaced and orphan }); removed")
else:
    print("Fix 2&3 NOT applied - pattern not found")
    # Try to find the section
    idx = content.find('_origShowSection')
    if idx != -1:
        print(f"  Found _origShowSection at {idx}")
        print(f"  Context: {repr(content[max(0,idx-100):idx+300])}")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done!")
