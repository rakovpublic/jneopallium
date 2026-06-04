param(
    [string]$ScenarioId = "baseline_foraging",
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..\..")
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $RootDir "target\jneopallium-autonomous-mind"
}

$Launcher = "com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindFullRunLauncher"
$Maven = if ([string]::IsNullOrWhiteSpace($env:MAVEN_CMD)) { "mvn" } else { $env:MAVEN_CMD }

Push-Location $RootDir
try {
    & $Maven -q "-DskipTests" install
    & $Maven -q -pl worker "-DincludeScope=runtime" dependency:build-classpath "-Dmdep.outputFile=target/autonomous-mind-classpath.txt"

    $WorkerJar = Join-Path $RootDir "worker\target\worker-1.0-SNAPSHOT.jar"
    $Deps = (Get-Content (Join-Path $RootDir "worker\target\autonomous-mind-classpath.txt") -Raw).Trim()
    $Classpath = $WorkerJar + [IO.Path]::PathSeparator + $Deps

    & java -cp $Classpath $Launcher $ScenarioId --output $OutputDir
    Write-Host "AutonomousMind manifest: $(Join-Path (Join-Path $OutputDir $ScenarioId) 'manifest.json')"
    Write-Host "AutonomousMind safety:   $(Join-Path (Join-Path $OutputDir $ScenarioId) 'safety_summary.json')"
}
finally {
    Pop-Location
}
