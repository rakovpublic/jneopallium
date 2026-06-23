param(
  [switch]$quick,
  [switch]$full,
  [switch]$offline,
  [int]$maxRows = 760,
  [int]$maxMemoryMb = 1024,
  [int]$seed = 1729,
  [switch]$forceRetrain
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$argsList = @()
if ($quick) { $argsList += "--quick" }
if ($full) { $argsList += "--full" }
if ($offline) { $argsList += "--offline" }
if ($forceRetrain) { $argsList += "--force-retrain" }
$argsList += @("--max-rows", "$maxRows", "--max-memory-mb", "$maxMemoryMb", "--seed", "$seed")
python (Join-Path $scriptDir "run_all.py") @argsList
