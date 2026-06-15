param(
    [string]$Scenario = "all"
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root

Get-Command java | Out-Null
Get-Command mvn | Out-Null
Get-Command python | Out-Null

mvn -q -pl worker -DskipTests package
python -m pip install -q -r scripts/demo-industrial-fmi/requirements.txt
python scripts/demo-industrial-fmi/run_demo.py $Scenario
