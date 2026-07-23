$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "build.ps1")

$outDir = Join-Path $PSScriptRoot "out"
$testClassesDir = Join-Path $outDir "test-classes"
$appJar = Join-Path $outDir "ProgrammableAutoClicker.jar"
$libDir = Join-Path $PSScriptRoot "lib"
$testSource = Join-Path $PSScriptRoot "tests\ElapsedTimerTest.java"

New-Item -ItemType Directory -Path $testClassesDir -Force | Out-Null

javac `
    -encoding UTF-8 `
    -cp "$appJar;$libDir\*" `
    -d $testClassesDir `
    $testSource

java -cp "$testClassesDir;$appJar;$libDir\*" ElapsedTimerTest

Write-Host "All tests passed."
