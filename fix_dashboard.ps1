$file = 'src/main/resources/templates/dashboardAdministrador.html'
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

# Find the broken part - the chart closes without proper ending
$oldPart = "            });`n    // Polling ligero"
$newPart = "            });`n          }`n        }).catch(()=>{/* silent */});`n    });`n`n    // Polling ligero"

if ($content.Contains($oldPart)) {
    $content = $content.Replace($oldPart, $newPart)
    [System.IO.File]::WriteAllText($file, $content, [System.Text.Encoding]::UTF8)
    Write-Host "Fixed successfully"
} else {
    Write-Host "Pattern not found, trying alternative..."
    # Show what's around setInterval
    $idx = $content.IndexOf("setInterval(cargarNotificaciones")
    if ($idx -gt 0) {
        Write-Host "Found setInterval at index $idx"
        Write-Host "Context: '$($content.Substring([Math]::Max(0,$idx-100), 200))'"
    }
}
