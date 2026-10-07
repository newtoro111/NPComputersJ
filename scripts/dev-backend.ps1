$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Get-Content (Join-Path $projectRoot '.env') | ForEach-Object {
 if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
$env:SPRING_PROFILES_ACTIVE='local'
$env:APP_ORIGIN='http://localhost:5173'
$env:JWT_PRIVATE_KEY=Join-Path $projectRoot 'secrets/private.pem'
$env:JWT_PUBLIC_KEY=Join-Path $projectRoot 'secrets/public.pem'
Set-Location (Join-Path $projectRoot 'backend')
& ./mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'
exit $LASTEXITCODE
