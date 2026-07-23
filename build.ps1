$ErrorActionPreference = "Stop"

$sourceDir = Join-Path $PSScriptRoot "src\main\java"
$outDir = Join-Path $PSScriptRoot "out"
$classesDir = Join-Path $outDir "classes"
$libDir = Join-Path $PSScriptRoot "lib"
$outLibDir = Join-Path $outDir "lib"
$jarPath = Join-Path $outDir "ProgrammableAutoClicker.jar"
$manifestPath = Join-Path $outDir "MANIFEST.MF"
$mainClass = "com.autoclicker.ProgrammableAutoClicker"
$jnaVersion = "5.19.1"
$dependencies = @(
    @{
        Name = "jna-$jnaVersion.jar"
        Url = "https://repo1.maven.org/maven2/net/java/dev/jna/jna/$jnaVersion/jna-$jnaVersion.jar"
    },
    @{
        Name = "jna-platform-$jnaVersion.jar"
        Url = "https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/$jnaVersion/jna-platform-$jnaVersion.jar"
    }
)

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    $adoptium = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($adoptium) {
        $env:Path = (Join-Path $adoptium.FullName "bin") + ";$env:Path"
    }
}

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw "javac was not found. Install a free JDK 21+ and make sure it is on PATH."
}

Remove-Item -LiteralPath $outDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $classesDir | Out-Null
New-Item -ItemType Directory -Path $libDir -Force | Out-Null
New-Item -ItemType Directory -Path $outLibDir -Force | Out-Null

foreach ($dependency in $dependencies) {
    $target = Join-Path $libDir $dependency.Name
    if (-not (Test-Path -LiteralPath $target)) {
        Write-Host "Downloading $($dependency.Name)"
        Invoke-WebRequest -Uri $dependency.Url -OutFile $target
    }
    Copy-Item -LiteralPath $target -Destination $outLibDir -Force
}

$sources = Get-ChildItem -Path $sourceDir -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if (-not $sources) {
    throw "No Java source files found."
}

$classpath = (Join-Path $libDir "*")
javac -encoding UTF-8 -cp $classpath -d $classesDir $sources

$manifest = @"
Manifest-Version: 1.0
Main-Class: $mainClass
Class-Path: lib/jna-$jnaVersion.jar lib/jna-platform-$jnaVersion.jar

"@
Set-Content -LiteralPath $manifestPath -Value $manifest -Encoding ASCII

jar --create --file $jarPath --manifest $manifestPath -C $classesDir .

Write-Host "Built $jarPath"
