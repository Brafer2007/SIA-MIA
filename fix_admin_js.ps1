$file = 'src/main/resources/templates/dashboardAdministrador.html'
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

# Find the broken section: from the chartUsuarios .then(data => { that has no closing
# to the orphan }); before SISTEMA DE NOTIFICACIONES
$broken = @'
        .then(data => {
          if (document.getElementById("chartUsuarios")) {
            new Chart(document.getElementById("chartUsuarios"), {
              type: "pie",
              data: { labels: Object.keys(data), datasets: [{ data: Object.values(data), backgroundColor: chartTheme.pieColors }] },
              options: { responsive: true, maintainAspectRatio: false }
            });
    // Polling ligero: cada 4s actualiza contador / animacion
    setInterval(cargarNotificaciones, 4000);
'@

$fixed = @'
        .then(data => {
          if (document.getElementById("chartUsuarios")) {
            new Chart(document.getElementById("chartUsuarios"), {
              type: "pie",
              data: { labels: Object.keys(data), datasets: [{ data: Object.values(data), backgroundColor: chartTheme.pieColors }] },
              options: { responsive: true, maintainAspectRatio: false }
            });
          }
        }).catch(()=>{/* silent */});
    });

    // Polling ligero: cada 4s actualiza contador / animacion
    setInterval(cargarNotificaciones, 4000);
'@

if ($content.Contains('setInterval(cargarNotificaciones, 4000);')) {
    Write-Host "Found setInterval - file seems already partially fixed or different structure"
} else {
    Write-Host "setInterval not found"
}

Write-Host "Content length: $($content.Length)"
