param(
    [string]$Scenario = "live_single_autonomous",
    [string]$Backend = "JNEO_BATTLESPACE",
    [switch]$Headless
)

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
$ArgsList = @(
    (Join-Path $RepoRoot "simulators/jneo-battlespace/supervisor/process_supervisor.py"),
    $Scenario,
    "--backend",
    $Backend
)
if ($Headless) {
    $ArgsList += "--headless"
}
python @ArgsList

