. "$PSScriptRoot/comun.ps1"
$securePassword = Read-Host 'Contraseña del administrador (mínimo 12 caracteres)' -AsSecureString
$credential = New-Object System.Management.Automation.PSCredential('admin', $securePassword)
$env:ADMIN_PASSWORD = $credential.GetNetworkCredential().Password
try {
    & mvn compile exec:java '-Dexec.mainClass=pe.edu.matricula.tools.BootstrapAdmin'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el administrador. Revisa MySQL, los scripts y la contraseña.' }
} finally { Remove-Item Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue }
