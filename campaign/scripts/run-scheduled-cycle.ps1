[CmdletBinding()]
param(
    [ValidateRange(1, 120)]
    [int]$TimeoutMinutes = 20
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$CampaignDir = Split-Path -Parent $PSScriptRoot
$Runner = Join-Path $CampaignDir '.venv\Scripts\jneo-campaign.exe'
$RuntimeDir = Join-Path $CampaignDir 'runtime'
$LogPath = Join-Path $RuntimeDir 'campaign.scheduler.log'
$StatusPath = Join-Path $RuntimeDir 'campaign.scheduler.status.json'
$RunId = [guid]::NewGuid().ToString('N')
$StartedAt = Get-Date
$Process = $null
$StdoutTask = $null
$StderrTask = $null
$ProcessStarted = $false
$ExitCode = $null
$OutputCopied = $false

function Write-SchedulerLog {
    param([Parameter(Mandatory = $true)][string]$Message)

    "[$((Get-Date).ToString('o'))] $Message" |
        Out-File -LiteralPath $LogPath -Append -Encoding utf8
}

function Write-SchedulerStatus {
    param(
        [Parameter(Mandatory = $true)][string]$State,
        [AllowNull()][Nullable[int]]$ProcessId,
        [AllowNull()][Nullable[int]]$ProcessExitCode,
        [AllowNull()][string]$Message,
        [AllowNull()][object]$CompletedAt
    )

    $status = [ordered]@{
        run_id = $RunId
        state = $State
        started_at = $StartedAt.ToString('o')
        heartbeat_at = (Get-Date).ToString('o')
        completed_at = if ($null -eq $CompletedAt) { $null } else { ([datetime]$CompletedAt).ToString('o') }
        process_id = $ProcessId
        exit_code = $ProcessExitCode
        message = $Message
    }
    $temporaryStatusPath = "$StatusPath.$PID.tmp"
    $status | ConvertTo-Json -Depth 3 |
        Set-Content -LiteralPath $temporaryStatusPath -Encoding utf8
    Move-Item -LiteralPath $temporaryStatusPath -Destination $StatusPath -Force
}

function Copy-ProcessOutput {
    param(
        [AllowNull()][string]$Stdout,
        [AllowNull()][string]$Stderr
    )

    foreach ($item in @(
        @{ Text = $Stdout; Prefix = 'stdout' },
        @{ Text = $Stderr; Prefix = 'stderr' }
    )) {
        if (-not [string]::IsNullOrWhiteSpace($item.Text)) {
            Write-SchedulerLog "$($item.Prefix) begin"
            $item.Text |
                Out-File -LiteralPath $LogPath -Append -Encoding utf8
            Write-SchedulerLog "$($item.Prefix) end"
        }
    }
}

New-Item -ItemType Directory -Path $RuntimeDir -Force | Out-Null

try {
    if (-not (Test-Path -LiteralPath $Runner)) {
        throw "Campaign runner not found: $Runner"
    }

    Write-SchedulerLog "cycle started; run_id=$RunId; timeout_minutes=$TimeoutMinutes"
    Write-SchedulerStatus -State 'STARTING' -ProcessId $null -ProcessExitCode $null -Message $null -CompletedAt $null

    $StartInfo = New-Object System.Diagnostics.ProcessStartInfo
    $StartInfo.FileName = $Runner
    $StartInfo.Arguments = 'run --once'
    $StartInfo.WorkingDirectory = $CampaignDir
    $StartInfo.UseShellExecute = $false
    $StartInfo.CreateNoWindow = $true
    $StartInfo.RedirectStandardOutput = $true
    $StartInfo.RedirectStandardError = $true

    $Process = New-Object System.Diagnostics.Process
    $Process.StartInfo = $StartInfo
    if (-not $Process.Start()) {
        throw 'Campaign process could not be started'
    }
    $ProcessStarted = $true
    $StdoutTask = $Process.StandardOutput.ReadToEndAsync()
    $StderrTask = $Process.StandardError.ReadToEndAsync()

    Write-SchedulerLog "campaign process started; run_id=$RunId; process_id=$($Process.Id)"
    Write-SchedulerStatus -State 'RUNNING' -ProcessId $Process.Id -ProcessExitCode $null -Message $null -CompletedAt $null

    $Deadline = $StartedAt.AddMinutes($TimeoutMinutes)
    $NextHeartbeat = (Get-Date).AddMinutes(1)
    while (-not $Process.WaitForExit(5000)) {
        $Now = Get-Date
        if ($Now -ge $Deadline) {
            Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
            $Process.WaitForExit(10000) | Out-Null
            $ExitCode = 124
            throw [System.TimeoutException]::new(
                "Campaign process exceeded the $TimeoutMinutes minute worker timeout"
            )
        }
        if ($Now -ge $NextHeartbeat) {
            $elapsedSeconds = [math]::Round(($Now - $StartedAt).TotalSeconds)
            Write-SchedulerStatus -State 'RUNNING' -ProcessId $Process.Id -ProcessExitCode $null -Message "elapsed_seconds=$elapsedSeconds" -CompletedAt $null
            $NextHeartbeat = $Now.AddMinutes(1)
        }
    }

    # WaitForExit() without a timeout ensures redirected streams are completely flushed.
    $Process.WaitForExit()
    $Process.Refresh()
    $ExitCode = [int]$Process.ExitCode
    $Stdout = $StdoutTask.GetAwaiter().GetResult()
    $Stderr = $StderrTask.GetAwaiter().GetResult()
    Copy-ProcessOutput -Stdout $Stdout -Stderr $Stderr
    $OutputCopied = $true

    if ($ExitCode -ne 0) {
        throw "Campaign process failed with exit code $ExitCode"
    }

    $CompletedAt = Get-Date
    Write-SchedulerLog "cycle completed; run_id=$RunId; process_id=$($Process.Id); exit_code=0; duration_seconds=$([math]::Round(($CompletedAt - $StartedAt).TotalSeconds, 1))"
    Write-SchedulerStatus -State 'COMPLETED' -ProcessId $Process.Id -ProcessExitCode 0 -Message $null -CompletedAt $CompletedAt
    exit 0
}
catch {
    if (-not $OutputCopied) {
        $Stdout = if (($null -ne $StdoutTask) -and $StdoutTask.IsCompleted) {
            $StdoutTask.GetAwaiter().GetResult()
        } else {
            $null
        }
        $Stderr = if (($null -ne $StderrTask) -and $StderrTask.IsCompleted) {
            $StderrTask.GetAwaiter().GetResult()
        } else {
            $null
        }
        Copy-ProcessOutput -Stdout $Stdout -Stderr $Stderr
    }
    if (($null -eq $ExitCode) -and $ProcessStarted -and $Process.HasExited) {
        $ExitCode = [int]$Process.ExitCode
    }
    if (($null -eq $ExitCode) -or ($ExitCode -eq 0)) {
        $ExitCode = 1
    }

    $CompletedAt = Get-Date
    $FailureState = if ($_.Exception -is [System.TimeoutException]) { 'TIMED_OUT' } else { 'FAILED' }
    $FailureMessage = "$($_.Exception.GetType().Name): $($_.Exception.Message)"
    Write-SchedulerLog "cycle failed; run_id=$RunId; state=$FailureState; exit_code=$ExitCode; duration_seconds=$([math]::Round(($CompletedAt - $StartedAt).TotalSeconds, 1)); error=$FailureMessage"
    Write-SchedulerStatus -State $FailureState -ProcessId $(if ($ProcessStarted) { $Process.Id } else { $null }) -ProcessExitCode $ExitCode -Message $FailureMessage -CompletedAt $CompletedAt
    exit $ExitCode
}
finally {
    if ($null -ne $Process) {
        $Process.Dispose()
    }
}
