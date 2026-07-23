$ErrorActionPreference = "Stop"

$appName = "ProgrammableAutoClicker"
$appVersion = "1.0.0"
$outDir = Join-Path $PSScriptRoot "out"
$distDir = Join-Path $PSScriptRoot "dist"
$jarPath = Join-Path $outDir "$appName.jar"
$packageInputDir = Join-Path $outDir "package-input"

& (Join-Path $PSScriptRoot "build.ps1")

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    $adoptium = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($adoptium) {
        $env:Path = (Join-Path $adoptium.FullName "bin") + ";$env:Path"
    }
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage was not found. Install a free JDK 21+ and make sure it is on PATH."
}

Remove-Item -LiteralPath $distDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $distDir | Out-Null
New-Item -ItemType Directory -Path $packageInputDir -Force | Out-Null
Copy-Item -LiteralPath $jarPath -Destination $packageInputDir -Force
Copy-Item -LiteralPath (Join-Path $outDir "lib") -Destination $packageInputDir -Recurse -Force

jpackage `
    --type app-image `
    --name $appName `
    --input $packageInputDir `
    --main-jar "$appName.jar" `
    --main-class "com.autoclicker.ProgrammableAutoClicker" `
    --dest $distDir `
    --app-version $appVersion `
    --description "Configurable Windows mouse and keyboard automation" `
    --vendor "Programmable Auto Clicker"

Write-Host "Created app image in $distDir"

$zipName = "$appName-v$appVersion-windows-x64.zip"
$zipPath = Join-Path $distDir $zipName
Compress-Archive -Path (Join-Path $distDir $appName) -DestinationPath $zipPath -CompressionLevel Optimal
$zipHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
$checksumPath = "$zipPath.sha256"
Set-Content -LiteralPath $checksumPath -Value "$zipHash  $zipName" -Encoding ASCII

Write-Host "Created release archive $zipPath"
Write-Host "Created SHA-256 checksum $checksumPath"

if (Get-Command candle.exe -ErrorAction SilentlyContinue) {
    jpackage `
        --type msi `
        --name $appName `
        --input $packageInputDir `
        --main-jar "$appName.jar" `
        --main-class "com.autoclicker.ProgrammableAutoClicker" `
        --dest $distDir `
        --win-menu `
        --win-shortcut `
        --app-version $appVersion `
        --description "Configurable Windows mouse and keyboard automation" `
        --vendor "Programmable Auto Clicker"

    Write-Host "Created MSI installer in $distDir"
} else {
    Write-Host "WiX Toolset was not found, so MSI packaging was skipped."
}
