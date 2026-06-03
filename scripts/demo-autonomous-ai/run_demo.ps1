param(
    [string]$ScenarioId = "baseline_foraging",
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..\..")
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $RootDir "target\jneopallium-autonomous-ai-demo"
}

$Launcher = "com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiDemoLauncher"
$Maven = if ([string]::IsNullOrWhiteSpace($env:MAVEN_CMD)) { "mvn" } else { $env:MAVEN_CMD }

Push-Location $RootDir
try {
    & $Maven -q -DskipTests=false install
    & $Maven -q -pl worker "-DincludeScope=runtime" dependency:build-classpath "-Dmdep.outputFile=target/autonomous-ai-classpath.txt"

    $WorkerJar = Join-Path $RootDir "worker\target\worker-1.0-SNAPSHOT.jar"
    $Deps = (Get-Content (Join-Path $RootDir "worker\target\autonomous-ai-classpath.txt") -Raw).Trim()
    $Classpath = $WorkerJar + [IO.Path]::PathSeparator + $Deps

    & java -cp $Classpath $Launcher $ScenarioId --output $OutputDir
    Write-Host "Autonomous AI demo manifest: $(Join-Path (Join-Path $OutputDir $ScenarioId) 'manifest.json')"
}
finally {
    Pop-Location
}
