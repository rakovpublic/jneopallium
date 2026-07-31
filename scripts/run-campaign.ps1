param(
    [ValidateSet('once', 'continuous', 'doctor', 'dashboard')]
    [string]$Action = 'once'
)

$ErrorActionPreference = 'Stop'
$CampaignDir = Join-Path $PSScriptRoot '..\campaign'
Push-Location $CampaignDir
try {
    if ($Action -eq 'doctor') {
        python -m jneo_campaign.cli.app doctor
    } elseif ($Action -eq 'dashboard') {
        python -m jneo_campaign.cli.app dashboard
    } elseif ($Action -eq 'continuous') {
        python -m jneo_campaign.cli.app run --continuous
    } else {
        python -m jneo_campaign.cli.app run --once
    }
} finally {
    Pop-Location
}
