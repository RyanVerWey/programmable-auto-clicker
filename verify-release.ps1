param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$appName = "ProgrammableAutoClicker"
$distDir = Join-Path $PSScriptRoot "dist"
$outDir = Join-Path $PSScriptRoot "out"
$zipName = "$appName-v$Version-windows-x64.zip"
$zipPath = Join-Path $distDir $zipName
$checksumPath = "$zipPath.sha256"
$smokeDir = Join-Path $outDir "release-smoke"

if (-not (Test-Path -LiteralPath $zipPath)) {
    throw "Release archive not found: $zipPath"
}
if (-not (Test-Path -LiteralPath $checksumPath)) {
    throw "Checksum file not found: $checksumPath"
}

$expectedHash = ((Get-Content -LiteralPath $checksumPath) -split "\s+")[0].ToLowerInvariant()
$actualHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($expectedHash -ne $actualHash) {
    throw "Checksum mismatch: expected $expectedHash, got $actualHash"
}

Remove-Item -LiteralPath $smokeDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $smokeDir | Out-Null
Expand-Archive -LiteralPath $zipPath -DestinationPath $smokeDir

$appDir = Join-Path $smokeDir $appName
$exePath = Join-Path $appDir "$appName.exe"
$runtimeDir = Join-Path $appDir "runtime"
$appFilesDir = Join-Path $appDir "app"

if (-not (Test-Path -LiteralPath $exePath)) {
    throw "Extracted archive is missing $appName.exe"
}
if (-not (Test-Path -LiteralPath $runtimeDir -PathType Container)) {
    throw "Extracted archive is missing its bundled Java runtime"
}
if (-not (Test-Path -LiteralPath $appFilesDir -PathType Container)) {
    throw "Extracted archive is missing its application files"
}

Start-Process -FilePath $exePath
$window = $null
$deadline = (Get-Date).AddSeconds(20)

while ((Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 250
    $window = Get-Process -Name $appName -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Path -eq $exePath -and
            $_.MainWindowTitle -eq "Programmable Auto Clicker v$Version"
        } |
        Select-Object -First 1
    if ($window) {
        break
    }
}

if (-not $window) {
    throw "Extracted application did not open with the expected v$Version title"
}

$releaseProcesses = Get-Process -Name $appName -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -eq $exePath }
$releaseProcesses | Stop-Process -Force
$releaseProcesses | Wait-Process -Timeout 10 -ErrorAction SilentlyContinue

for ($attempt = 1; $attempt -le 5; $attempt++) {
    try {
        Remove-Item -LiteralPath $smokeDir -Recurse -Force
        break
    } catch {
        if ($attempt -eq 5) {
            throw
        }
        Start-Sleep -Milliseconds 500
    }
}

Write-Host "PASS: $zipName"
Write-Host "  SHA-256: $actualHash"
Write-Host "  Bundled runtime: present"
Write-Host "  Smoke launch: Programmable Auto Clicker v$Version"
