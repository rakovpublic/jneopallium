# Live demo: label-free detection + continuous feedback learning, on the real
# Java neurons. Generates a telemetry replay (Python) and runs it through the
# deployed model (Java).
#
#   1. python make_demo_replay.py   -> target/.../replay.json
#   2. compile + run SelfSupervisedMaintenanceDemo against the worker classpath
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$root = (Resolve-Path "$PSScriptRoot\..\..\..").Path

python make_demo_replay.py

$classes = Join-Path $root "worker\target\classes"
# reuse a dependency classpath from a prior build if present, else ask Maven
$cpFile = $null
foreach ($f in @("worker\target\ssmaint-demo-cp.txt", "worker\target\industrial-cp.txt")) {
    $p = Join-Path $root $f
    if (Test-Path $p) { $cpFile = $p; break }
}
if (-not (Test-Path $classes) -or $null -eq $cpFile) {
    Write-Host "Compiling worker and resolving classpath via Maven..."
    $cpFile = Join-Path $root "worker\target\ssmaint-demo-cp.txt"
    Push-Location $root
    mvn -q -pl worker -am compile
    mvn -q -pl worker dependency:build-classpath "-Dmdep.outputFile=$cpFile"
    Pop-Location
}
$cp = "$classes;" + (Get-Content $cpFile -Raw).Trim()

$build = Join-Path $PSScriptRoot "build"
New-Item -ItemType Directory -Force -Path $build | Out-Null
# compile the ssmaint neurons from source + the demo, so this runs from a fresh
# checkout without a full Maven build; build\ goes first on the classpath
$ssmaintRoot = Join-Path $root "worker\src\main\java\com\rakovpublic\jneuropallium\worker"
$srcs = @(Get-ChildItem -Path $ssmaintRoot -Recurse -Filter *.java |
          Where-Object { $_.FullName -match 'ssmaint' } | ForEach-Object { $_.FullName })
$srcs += (Join-Path $PSScriptRoot "SelfSupervisedMaintenanceDemo.java")
javac -cp $cp -d $build $srcs

$fitted = Join-Path $root "worker\src\main\resources\model\self-supervised-maintenance\fitted-model.json"
$replay = Join-Path $root "target\jneopallium-ss-maintenance-demo\replay.json"
java -cp "$build;$cp" SelfSupervisedMaintenanceDemo $fitted $replay
