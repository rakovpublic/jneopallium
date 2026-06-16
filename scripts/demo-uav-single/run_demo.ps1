param(
    [string]$ScenarioId = "all",
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..\..")
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $RootDir "target\jneopallium-uav-single"
}

$Launcher = "com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleDemoLauncher"
$Maven = if ([string]::IsNullOrWhiteSpace($env:MAVEN_CMD)) { "mvn" } else { $env:MAVEN_CMD }
$Scenarios = @(
    "autonomous_success",
    "autonomous_priority_change",
    "confirm_approved",
    "confirm_denied",
    "confirm_timeout",
    "low_battery_rth",
    "geofence_veto",
    "lost_heartbeat",
    "poor_visibility",
    "duplicate_confirmation"
)

Push-Location $RootDir
try {
    & $Maven -q -pl worker "-Dtest=UavSingle*Test" test
    & $Maven -q "-DskipTests" install
    & $Maven -q -pl worker "-DincludeScope=runtime" dependency:build-classpath "-Dmdep.outputFile=target/uav-single-classpath.txt"

    $WorkerJar = Join-Path $RootDir "worker\target\worker-1.0-SNAPSHOT.jar"
    $Deps = (Get-Content (Join-Path $RootDir "worker\target\uav-single-classpath.txt") -Raw).Trim()
    $Classpath = $WorkerJar + [IO.Path]::PathSeparator + $Deps

    & java -cp $Classpath $Launcher $ScenarioId --output $OutputDir

    $SummaryPath = Join-Path $OutputDir "summary.json"
    if (-not (Test-Path $SummaryPath)) {
        throw "missing UAV single summary: $SummaryPath"
    }
    if ($ScenarioId -eq "all") {
        foreach ($Scenario in $Scenarios) {
            $ManifestPath = Join-Path (Join-Path $OutputDir $Scenario) "manifest.json"
            if (-not (Test-Path $ManifestPath)) {
                throw "missing UAV single manifest: $ManifestPath"
            }
        }
    } else {
        $ManifestPath = Join-Path (Join-Path $OutputDir $ScenarioId) "manifest.json"
        if (-not (Test-Path $ManifestPath)) {
            throw "missing UAV single manifest: $ManifestPath"
        }
    }

    Write-Host "UAV single summary:  $SummaryPath"
    Write-Host "UAV single artifacts: $OutputDir"
}
finally {
    Pop-Location
}
