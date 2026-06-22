param(
    [string]$Map = "Town10HD",
    [int]$RpcPort = 2000,
    [int]$Width = 1920,
    [int]$Height = 1080,
    [ValidateSet("Low", "Epic")]
    [string]$Quality = "Epic",
    [string]$CarlaAirRoot = "$PSScriptRoot\..\..\.codex-tools\carla-air\CarlaAir-v0.1.7-Windows11-x86_64\WindowsNoEditor"
)

$ErrorActionPreference = "Stop"

$exe = Join-Path $CarlaAirRoot "CarlaUE4.exe"
if (-not (Test-Path $exe)) {
    throw "CarlaAir executable not found: $exe"
}

$arguments = @(
    $Map,
    "-windowed",
    "-ResX=$Width",
    "-ResY=$Height",
    "-carla-rpc-port=$RpcPort",
    "-quality-level=$Quality",
    "-NoVSync"
)

Write-Host "Starting CarlaAir visible renderer:"
Write-Host "  $exe $($arguments -join ' ')"
Start-Process -FilePath $exe -ArgumentList $arguments -WorkingDirectory $CarlaAirRoot
