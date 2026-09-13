param(
    [ValidateSet('Build', 'Start', 'Stop', 'Status')]
    [string]$Action = 'Start',
    [ValidateRange(1024, 65535)]
    [int]$Port = 8080,
    [switch]$Demo
)
$ErrorActionPreference = 'Stop'
$repoPath = Split-Path -Parent $PSScriptRoot
$localPath = Join-Path $repoPath '.local'
$javaPath = Join-Path $repoPath '.tools\jdk-11.0.32.1+1\bin\java.exe'
$mavenPath = Join-Path $repoPath '.tools\apache-maven-3.9.16\bin\mvn.cmd'
$jarPath = Join-Path $PSScriptRoot 'target\shop-api-0.0.1-SNAPSHOT.jar'
$configPath = Join-Path $PSScriptRoot 'config\local.properties'
$pidPath = Join-Path $localPath 'backend-process.json'

function Get-LocalBackend {
    if (!(Test-Path -LiteralPath $pidPath)) { return $null }
    $record = Get-Content -LiteralPath $pidPath -Raw | ConvertFrom-Json
    $process = Get-Process -Id $record.id -ErrorAction SilentlyContinue
    if (!$process) { return $null }
    if ($process.Path -ne $javaPath -or $process.StartTime.ToUniversalTime().Ticks.ToString() -ne $record.startTicks) {
        throw 'Saved PID belongs to a different process; refusing to use it.'
    }
    return $process
}

if ($Action -eq 'Stop') {
    $process = Get-LocalBackend
    if ($process) {
        Stop-Process -Id $process.Id
        $process.WaitForExit(15000) | Out-Null
        if (!$process.HasExited) { throw 'Backend did not stop within 15 seconds.' }
    }
    if (Test-Path -LiteralPath $pidPath) { Remove-Item -LiteralPath $pidPath }
    Write-Output 'Local backend stopped.'
    exit 0
}
if ($Action -eq 'Status') {
    $process = Get-LocalBackend
    if (!$process) { Write-Output 'Local backend is stopped.'; exit 0 }
    $record = Get-Content -LiteralPath $pidPath -Raw | ConvertFrom-Json
    $response = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$($record.port)/product" -TimeoutSec 10
    Write-Output "PID $($process.Id), HTTP $($response.StatusCode), http://127.0.0.1:$($record.port)/product"
    Write-Output $response.Content
    exit 0
}
if (!(Test-Path -LiteralPath $javaPath) -or !(Test-Path -LiteralPath $mavenPath)) {
    throw 'Project-local tools are missing. Run .\setup-local.ps1 first.'
}
New-Item -ItemType Directory -Path $localPath -Force | Out-Null

# These changes affect only this process and its children and are restored below.
# Clear Spring/JVM overrides so saved connection settings cannot replace local.properties.
$savedEnvironment = @{}
$environmentNames = @('JAVA_HOME','MAVEN_HOME','M2_HOME','MAVEN_USER_HOME','MAVEN_OPTS','MAVEN_ARGS',
    'JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS')
$environmentNames += @(Get-ChildItem Env: | Where-Object { $_.Name -like 'SPRING_*' } | ForEach-Object { $_.Name })
foreach ($name in $environmentNames) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    [Environment]::SetEnvironmentVariable($name, $null, 'Process')
}
try {
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaPath)
    $env:MAVEN_USER_HOME = Join-Path $repoPath '.tools\maven-home'
    Push-Location -LiteralPath $PSScriptRoot
    try {
        if ($Action -eq 'Build') {
            if (Get-LocalBackend) { throw 'Stop the local backend before rebuilding its executable jar.' }
            & $mavenPath --batch-mode --no-transfer-progress --settings config/maven-settings.xml --global-settings config/maven-settings.xml "-Dmaven.repo.local=$repoPath\.tools\repository" '-Dmaven.test.failure.ignore=false' clean verify
            if ($LASTEXITCODE -ne 0) { throw "Build failed with exit code $LASTEXITCODE" }
        } else {
            $existing = Get-LocalBackend
            if ($existing) { throw "Local backend is already running as PID $($existing.Id)." }
            if (!(Test-Path -LiteralPath $jarPath)) { throw 'Run .\local.ps1 Build first.' }
            if (!(Test-Path -LiteralPath $configPath)) { throw 'The isolated local configuration is missing.' }
            $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
            try { $listener.Start() } finally { $listener.Stop() }
            $configUri = ([Uri]$configPath).AbsoluteUri
            $process = Start-Process -FilePath $javaPath -ArgumentList @(
                '-Dspring.devtools.restart.enabled=false', '-jar', ('"' + $jarPath + '"'),
                ('"--spring.config.location=' + $configUri + '"'), '--spring.profiles.active=local',
                '--server.address=127.0.0.1', "--server.port=$Port",
                ("--shop.demo.enabled=" + $Demo.IsPresent.ToString().ToLowerInvariant())
            ) -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru `
              -RedirectStandardOutput (Join-Path $localPath 'backend.stdout.log') `
              -RedirectStandardError (Join-Path $localPath 'backend.stderr.log')
            @{ id=$process.Id; startTicks=$process.StartTime.ToUniversalTime().Ticks.ToString(); port=$Port } |
                ConvertTo-Json | Set-Content -LiteralPath $pidPath
            $ready = $false
            for ($attempt = 0; $attempt -lt 45; $attempt++) {
                $process.Refresh()
                if ($process.HasExited) { throw 'Backend exited. Read ..\.local\backend.stdout.log and backend.stderr.log.' }
                try {
                    $response = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$Port/product" -TimeoutSec 1
                    if ($response.StatusCode -eq 200) { $ready = $true; break }
                } catch { Start-Sleep -Milliseconds 500 }
            }
            if (!$ready) { throw "Backend PID $($process.Id) is still starting. Check the logs or run .\local.ps1 Stop." }
            Write-Output "Started PID $($process.Id). API: http://127.0.0.1:$Port/product (HTTP $($response.StatusCode))"
        }
    } finally { Pop-Location }
} finally {
    foreach ($name in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
}
