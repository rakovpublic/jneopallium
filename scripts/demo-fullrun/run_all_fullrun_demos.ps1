param(
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..\..")
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $RootDir "target\jneopallium-fullrun-demos"
}

$Launcher = "com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoFullRunLauncher"
$Maven = if ([string]::IsNullOrWhiteSpace($env:MAVEN_CMD)) { "mvn" } else { $env:MAVEN_CMD }

Push-Location $RootDir
try {
    & $Maven -q -DskipTests=false clean install
    & $Maven -q -pl worker "-DincludeScope=runtime" dependency:build-classpath "-Dmdep.outputFile=target/fullrun-classpath.txt"

    $WorkerJar = Join-Path $RootDir "worker\target\worker-1.0-SNAPSHOT.jar"
    $Deps = (Get-Content (Join-Path $RootDir "worker\target\fullrun-classpath.txt") -Raw).Trim()
    $Classpath = $WorkerJar + [IO.Path]::PathSeparator + $Deps

    & java -cp $Classpath $Launcher all --output $OutputDir
    Write-Host "Full-run demo summary: $(Join-Path $OutputDir 'summary.json')"
}
finally {
    Pop-Location
}
