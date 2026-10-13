$ErrorActionPreference = 'Stop'
$projectDir = Split-Path $PSScriptRoot -Parent
Set-Location $projectDir
$configPath = Join-Path $projectDir 'config/local.ps1'
if (-not (Test-Path $configPath)) { throw 'Copia config/local.ps1.example como config/local.ps1 y completa los valores.' }
. $configPath
$javaVersion = (& cmd /c 'java -version 2>&1' | Out-String)
if ($javaVersion -notmatch 'version "21\.') { throw 'Este proyecto requiere JDK 21. Revisa java -version y JAVA_HOME.' }
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD) -or $env:DB_PASSWORD -eq 'REEMPLAZA_CON_TU_CLAVE_MYSQL') { throw 'Completa DB_PASSWORD en config/local.ps1.' }
