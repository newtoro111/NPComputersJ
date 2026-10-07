$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$secretRoot = Join-Path $projectRoot 'secrets'
$environmentPath = Join-Path $projectRoot '.env'

function New-Secret {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    return ([System.BitConverter]::ToString($bytes) -replace '-', '').ToLowerInvariant()
}

Write-Output 'Preparing NP Computers local development environment...'

# ----------------------------------------------------------------------
# Create the local secrets directory.
# ----------------------------------------------------------------------
New-Item -ItemType Directory -Force -Path $secretRoot | Out-Null

$privateKey = Join-Path $secretRoot 'private.pem'
$publicKey  = Join-Path $secretRoot 'public.pem'

# ----------------------------------------------------------------------
# Generate the JWT RSA key pair.
#
# Use Docker so Windows developers do not need PowerShell 7 or OpenSSL
# installed locally. The resulting files are mounted into the Spring
# Boot container by Docker Compose.
# ----------------------------------------------------------------------
if (-not (Test-Path $privateKey) -or -not (Test-Path $publicKey)) {

    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker is required to generate the local JWT signing keys.'
    }

    Write-Output 'Generating local JWT RSA key pair...'

    $resolvedSecretRoot = (Resolve-Path $secretRoot).Path

    docker run --rm `
        -v "${resolvedSecretRoot}:/keys" `
        alpine:3.20 `
        sh -c "apk add --no-cache openssl >/dev/null && openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out /keys/private.pem && openssl pkey -in /keys/private.pem -pubout -out /keys/public.pem && chmod 644 /keys/private.pem /keys/public.pem"

    if ($LASTEXITCODE -ne 0) {
        throw 'RSA key generation failed.'
    }
}
else {
    Write-Output 'JWT RSA key pair already exists; leaving it unchanged.'
}

# ----------------------------------------------------------------------
# Create the local environment file only when one does not already exist.
# Existing developer passwords and database settings are preserved.
# ----------------------------------------------------------------------
if (-not (Test-Path $environmentPath)) {

    Write-Output 'Creating .env with random local-development secrets...'

    $lines = @(
        ('POSTGRES_PASSWORD=' + (New-Secret))
        ('DB_PASSWORD=' + (New-Secret))
        ('MIGRATION_PASSWORD=' + (New-Secret))
        'ADMIN_EMAIL=admin@npcomputers.local'
        ('ADMIN_PASSWORD=' + (New-Secret))
        'APP_ORIGIN=http://localhost:8080'
    )

    [IO.File]::WriteAllLines($environmentPath, $lines)
}
else {
    Write-Output '.env already exists; leaving it unchanged.'
}

Write-Output ''
Write-Output 'Setup ready.'
Write-Output 'Local credentials and cryptographic keys are stored in ignored files.'
Write-Output 'Use the ADMIN_EMAIL and ADMIN_PASSWORD values in .env for the demo administrator.'
Write-Output ''
Write-Output 'Start NP Computers with:'
Write-Output 'docker compose --env-file ".\.env" -f ".\infra\compose.yaml" up --build'