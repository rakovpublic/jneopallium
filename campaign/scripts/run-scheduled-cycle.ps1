$ErrorActionPreference = 'Stop'

$CampaignDir = Split-Path -Parent $PSScriptRoot
$Runner = Join-Path $CampaignDir '.venv\Scripts\jneo-campaign.exe'
$RuntimeDir = Join-Path $CampaignDir 'runtime'
$LogPath = Join-Path $RuntimeDir 'campaign.scheduler.log'

if (-not (Test-Path -LiteralPath $Runner)) {
    throw "Campaign runner not found: $Runner"
}

New-Item -ItemType Directory -Path $RuntimeDir -Force | Out-Null
Push-Location $CampaignDir
try {
    "[$((Get-Date).ToString('o'))] cycle started" | Out-File -LiteralPath $LogPath -Append -Encoding utf8
    & $Runner run --once 2>&1 | Out-File -LiteralPath $LogPath -Append -Encoding utf8
    if ($LASTEXITCODE -ne 0) {
        throw "Campaign cycle failed with exit code $LASTEXITCODE"
    }
    "[$((Get-Date).ToString('o'))] cycle completed" | Out-File -LiteralPath $LogPath -Append -Encoding utf8
} finally {
    Pop-Location
}
