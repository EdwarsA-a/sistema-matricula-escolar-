. "$PSScriptRoot/comun.ps1"
$startScript = Join-Path $env:WILDFLY_HOME 'bin/standalone.bat'
if (-not (Test-Path $startScript)) { throw 'Revisa WILDFLY_HOME: no se encontró bin/standalone.bat.' }
& $startScript
