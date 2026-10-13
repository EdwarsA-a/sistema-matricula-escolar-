. "$PSScriptRoot/comun.ps1"
& mvn clean package
if ($LASTEXITCODE -ne 0) { throw 'La compilación o las pruebas fallaron.' }
$deployDir = Join-Path $env:WILDFLY_HOME 'standalone/deployments'
if (-not (Test-Path $deployDir)) { throw 'Revisa WILDFLY_HOME.' }
Copy-Item 'target/matricula.war' (Join-Path $deployDir 'matricula.war') -Force
Set-Content (Join-Path $deployDir 'matricula.war.dodeploy') ''
Write-Host 'Solicitado despliegue. Revisa el registro de WildFly y abre http://localhost:8080/matricula/login'
