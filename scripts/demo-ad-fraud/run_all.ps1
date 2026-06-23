param(
  [switch]$quick,
  [switch]$full,
  [switch]$offline,
  [int]$maxRows = 12000,
  [int]$maxMemoryMb = 1024,
  [int]$seed = 1729,
  [string]$firstPartyLabels = "",
  [switch]$forceRetrain
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$argsList = @()
if ($quick) { $argsList += "--quick" }
if ($full) { $argsList += "--full" }
if ($offline) { $argsList += "--offline" }
if ($forceRetrain) { $argsList += "--force-retrain" }
if ($firstPartyLabels -ne "") { $argsList += @("--first-party-labels", "$firstPartyLabels") }
$argsList += @("--max-rows", "$maxRows", "--max-memory-mb", "$maxMemoryMb", "--seed", "$seed")
python (Join-Path $scriptDir "run_all.py") @argsList
