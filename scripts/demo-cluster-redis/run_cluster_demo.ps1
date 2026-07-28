<#
.SYNOPSIS
    Runs the Redis cluster demo: one master, N workers, all model and signal state in Redis.

.DESCRIPTION
    Starts (or reuses) a local Redis, seeds the demo net, boots the master, attaches the requested
    number of workers and reports what each worker was asked to do.

    The point of the demo is the size and shape of the master to worker payload: every assignment
    is a neuron range of one layer plus Redis coordinates - never model or signal data - so the
    payload stays a few hundred bytes no matter how large the model is.

.EXAMPLE
    scripts\demo-cluster-redis\run_cluster_demo.ps1 -Workers 3 -Neurons 600

.EXAMPLE
    scripts\demo-cluster-redis\run_cluster_demo.ps1 -Workers 3 -Neurons 6000 -SkipBuild
    Same payload, ten times the model.
#>
[CmdletBinding()]
param(
    [int]$Workers = 3,
    [int]$Layers = 3,
    [int]$Neurons = 600,
    [int]$ResultNeurons = 50,
    [int]$Epochs = 3,
    [int]$Partitions = 4,
    [int]$Threads = 2,
    [int]$RedisPort = 6379,
    [int]$MasterPort = 8080,
    [string]$Net = "demo09",
    [switch]$SkipBuild,
    [switch]$KeepRunning
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$runDir = Join-Path $repoRoot "target\demo-cluster-redis"
$redisHome = Join-Path $env:LOCALAPPDATA "jneopallium-demo\redis"
$masterUrl = "http://127.0.0.1:$MasterPort"
$processes = @()

function Write-Phase([string]$text) {
    Write-Host ""
    Write-Host "=== $text ===" -ForegroundColor Cyan
}

function Get-MasterState {
    return Invoke-RestMethod -Uri "$masterUrl/debug/state" -TimeoutSec 30
}

function Wait-For([scriptblock]$condition, [int]$timeoutSeconds, [string]$what) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            if (& $condition) { return $true }
        } catch {
            # not ready yet
        }
        Start-Sleep -Milliseconds 400
    }
    throw "Timed out after ${timeoutSeconds}s waiting for $what"
}

New-Item -ItemType Directory -Force $runDir | Out-Null

# ------------------------------------------------------------------ redis
Write-Phase "Redis"
$redisCli = Join-Path $redisHome "redis-cli.exe"
if (-not (Test-Path $redisCli)) {
    throw "Redis is not installed at $redisHome - see docs/demo-cluster-redis/README.md for the one-time setup."
}
function Test-Redis {
    # redis-cli writes its connection failure to stderr, which would trip $ErrorActionPreference.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try { return (& $redisCli -p $RedisPort ping 2>$null) -eq "PONG" }
    catch { return $false }
    finally { $ErrorActionPreference = $previous }
}
if (-not (Test-Redis)) {
    $conf = Join-Path $env:LOCALAPPDATA "jneopallium-demo\redis-demo.conf"
    if (-not (Test-Path $conf)) { throw "Missing $conf - see docs/demo-cluster-redis/README.md" }
    Start-Process -FilePath (Join-Path $redisHome "redis-server.exe") -ArgumentList $conf -WorkingDirectory $redisHome -WindowStyle Hidden
    Start-Sleep -Seconds 2
    if (-not (Test-Redis)) { throw "Could not start redis on 127.0.0.1:$RedisPort" }
}
Write-Host "redis on 127.0.0.1:$RedisPort -> PONG"

# ------------------------------------------------------------------ build
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw "Maven is not on PATH" }
if (-not $SkipBuild) {
    Write-Phase "Build"
    & mvn -B -q -DskipTests -f (Join-Path $repoRoot "pom.xml") install
    if ($LASTEXITCODE -ne 0) { throw "Build failed" }
}

$classpathFile = Join-Path $runDir "worker-classpath.txt"
if (-not (Test-Path $classpathFile)) {
    & mvn -B -q -f (Join-Path $repoRoot "pom.xml") -pl worker dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
    if ($LASTEXITCODE -ne 0) { throw "Cannot resolve the worker classpath" }
}
$workerClasspath = (Join-Path $repoRoot "worker\target\classes") + ";" + (Get-Content $classpathFile -Raw).Trim()
$workerJarUrl = ([System.Uri]((Resolve-Path (Join-Path $repoRoot "worker\target\worker-1.0-SNAPSHOT.jar")).Path)).AbsoluteUri
$masterWar = Join-Path $repoRoot "master\target\jneuronnetmaster.war"

# Pass the context as a file: Entry resolves a path into the JSON itself, which avoids the
# shell mangling the quotes of an inline JSON argument.
$contextPath = Join-Path $runDir "context.json"
"{`"host`":`"127.0.0.1`",`"port`":$RedisPort,`"neuronNetName`":`"$Net`"}" | Out-File -FilePath $contextPath -Encoding ascii -NoNewline

# ------------------------------------------------------------------ seed
Write-Phase "Phase 1 - seed the model into Redis"
& java -cp $workerClasspath com.rakovpublic.jneuropallium.worker.demo.cluster.ClusterDemoSeeder `
    --host 127.0.0.1 --port $RedisPort --net $Net --layers $Layers --neurons $Neurons `
    --result $ResultNeurons --epochs $Epochs --partitions $Partitions `
    --master $masterUrl --threads $Threads --flush
if ($LASTEXITCODE -ne 0) { throw "Seeding failed" }
Write-Host "redis keys holding the whole model: $((& $redisCli -p $RedisPort keys "$Net`_*").Count)"

# ------------------------------------------------------------------ master
Write-Phase "Phase 2 - start the master"
$masterLog = Join-Path $runDir "master.log"
$masterProcess = Start-Process -FilePath "java" -ArgumentList @("-jar", $masterWar, "--server.port=$MasterPort") `
    -RedirectStandardOutput $masterLog -RedirectStandardError "$masterLog.err" -PassThru -WindowStyle Hidden
$processes += $masterProcess
Wait-For { (Get-MasterState) -ne $null } 120 "the master to accept requests"
Write-Host "master up on $masterUrl (pid $($masterProcess.Id)), log: $masterLog"

Write-Phase "Phase 3 - configure the master"
& java -cp $workerClasspath com.rakovpublic.jneuropallium.worker.demo.cluster.ClusterDemoConfigurator `
    --host 127.0.0.1 --port $RedisPort --net $Net --master $masterUrl --partitions $Partitions --threads $Threads
if ($LASTEXITCODE -ne 0) { throw "Configuration failed" }
Get-MasterState | ForEach-Object { $_.layers } | ForEach-Object {
    Write-Host ("  position {0,-3} layerId {1,-12} neurons {2}" -f $_.position, $_.layerId, $_.size)
}

# ------------------------------------------------------------ the payload
Write-Phase "Phase 4 - what the master actually sends a worker"
$probe = Invoke-WebRequest -Uri "$masterUrl/nodeManager/nextRun" -Method POST -ContentType "application/json" `
    -Body '{"nodeName":"payload-probe"}' -TimeoutSec 30
Write-Host $probe.Content
Write-Host ("payload: {0} bytes for a model of {1} neurons" -f $probe.RawContentLength, ($Layers * $Neurons + $ResultNeurons)) -ForegroundColor Green
# Asking for an assignment takes a real one. Hand it straight back the way a worker does,
# otherwise the probe counts as a busy node and the cluster waits for it until the node timeout.
Invoke-RestMethod -Uri "$masterUrl/input/callback" -Method POST -ContentType "application/json" `
    -Body '{"name":"payload-probe","signals":{},"discriminator":false}' -TimeoutSec 30 | Out-Null

# ------------------------------------------------------------------ workers
Write-Phase "Phase 5 - attach $Workers workers"
for ($i = 1; $i -le $Workers; $i++) {
    $workerLog = Join-Path $runDir "worker-$i.log"
    $workerProcess = Start-Process -FilePath "java" `
        -ArgumentList @("-cp", $workerClasspath, "-Djneuropallium.node.name=worker-$i",
                        "-Dorg.apache.logging.log4j.level=INFO",
                        "com.rakovpublic.jneuropallium.worker.application.Entry",
                        "http", $workerJarUrl,
                        "com.rakovpublic.jneuropallium.worker.util.RedisContext", $contextPath) `
        -RedirectStandardOutput $workerLog -RedirectStandardError "$workerLog.err" -PassThru -WindowStyle Hidden
    $processes += $workerProcess
    Write-Host "worker-$i started (pid $($workerProcess.Id)), log: $workerLog"
}
Wait-For { ((Get-MasterState).nodes.Count -ge $Workers) } 60 "all workers to register"

Write-Phase "Phase 6 - concurrent partitions"
$snapshot = $null
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    $busy = @((Get-MasterState).nodes | Where-Object { $_.idle -eq $false -and $null -ne $_.layerId })
    if ($busy.Count -ge 2) { $snapshot = $busy; break }
    Start-Sleep -Milliseconds 150
}
if ($snapshot) {
    Write-Host "caught live:"
    $snapshot | Sort-Object start | ForEach-Object {
        Write-Host ("  {0,-10} layer {1,-12} neurons [{2},{3})" -f $_.name, $_.layerId, $_.start, $_.end)
    }
} else {
    Write-Host "  layers finished too fast to catch two workers mid-flight; reading the logs instead"
}

# Deterministic check: every worker logs the partition it was given, so the assignments for one
# layer can be replayed from the logs and checked for gaps and overlaps.
$assignments = @()
for ($i = 1; $i -le $Workers; $i++) {
    $workerLog = Join-Path $runDir "worker-$i.log"
    if (-not (Test-Path $workerLog)) { continue }
    foreach ($line in Select-String -Path $workerLog -Pattern 'Processing layer (-?\d+) partition \[(\d+),(\d+)\)') {
        $groups = $line.Matches[0].Groups
        $assignments += [pscustomobject]@{
            worker = "worker-$i"
            layer  = [int]$groups[1].Value
            start  = [long]$groups[2].Value
            end    = [long]$groups[3].Value
        }
    }
}
$layerUnderTest = $assignments | Where-Object { $_.layer -ge 0 -and $_.layer -ne 2147483647 } |
    Group-Object layer | Sort-Object Count -Descending | Select-Object -First 1
if ($layerUnderTest) {
    $first = @($layerUnderTest.Group | Select-Object -First ($Partitions) | Sort-Object start)
    Write-Host ("first pass over layer {0}:" -f $layerUnderTest.Name)
    $first | ForEach-Object { Write-Host ("  {0,-10} neurons [{1},{2})" -f $_.worker, $_.start, $_.end) }
    $tiles = $true
    if ($first[0].start -ne 0) { $tiles = $false }
    for ($i = 1; $i -lt $first.Count; $i++) {
        if ($first[$i].start -ne $first[$i - 1].end) { $tiles = $false }
    }
    if ($first[-1].end -ne $Neurons) { $tiles = $false }
    $distinct = (@($first | Select-Object -ExpandProperty worker | Sort-Object -Unique)).Count
    Write-Host ("  partitions tile [0,$Neurons) with no gap or overlap: {0}" -f $tiles) -ForegroundColor Green
    Write-Host ("  spread across {0} distinct workers" -f $distinct) -ForegroundColor Green
}

Write-Phase "Phase 7 - results"
# An epoch ends when the result layer has been processed by every partition, so wait for the
# master to report a completed run before asking for results.
$results = $null
$deadline = (Get-Date).AddSeconds(300)
while ((Get-Date) -lt $deadline -and $null -eq $results) {
    foreach ($epoch in 0..6) {
        foreach ($loop in 0..2) {
            try {
                $candidate = Invoke-RestMethod -Uri "$masterUrl/nodeManager/getResults?loop=$loop&epoch=$epoch" -TimeoutSec 5
                if ($candidate) {
                    $results = $candidate
                    Write-Host "epoch $epoch loop $loop -> $($candidate.Count) result neurons" -ForegroundColor Green
                    Write-Host ("  sample: {0}" -f (($candidate[0] | Select-Object neuronId, layerRole, neuronLabel) | ConvertTo-Json -Compress))
                    break
                }
            } catch { }
        }
        if ($results) { break }
    }
    if (-not $results) { Start-Sleep -Milliseconds 750 }
}
if (-not $results) { Write-Warning "No results yet; inspect the logs in $runDir" }

Write-Phase "Redis contents"
$keys = & $redisCli -p $RedisPort keys "$Net`_*"
$keys | ForEach-Object {
    if ($_ -match '^.+_history_(-?\d+)_') { "history (layer $($matches[1]))" }
    elseif ($_ -match '^.+_(signals|layer_neurons|layer_index|layerIds|properties|input)') { $matches[1] }
    else { "other" }
} | Group-Object | Sort-Object Name | ForEach-Object { Write-Host ("  {0,-24} {1}" -f $_.Name, $_.Count) }

if (-not $KeepRunning) {
    Write-Phase "Shutting down"
    foreach ($process in $processes) {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
    }
    Write-Host "master and workers stopped; redis left running"
} else {
    Write-Host ""
    Write-Host "Left running. Stop with: Stop-Process -Id $($processes.Id -join ',') -Force"
}
