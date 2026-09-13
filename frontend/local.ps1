param([ValidateSet('Install','Build','Test','Lint','Start','Stop','Status')][string]$Action='Start')
$ErrorActionPreference='Stop'
$repoPath=Split-Path -Parent $PSScriptRoot
$nodeDir=Join-Path $repoPath '.tools\node-v10.24.1-win-x64'
$nodePath=Join-Path $nodeDir 'node.exe'
$localPath=Join-Path $repoPath '.local'
$pidPath=Join-Path $localPath 'frontend-process.json'
function Get-Frontend {
    if (!(Test-Path -LiteralPath $pidPath)) { return $null }
    $record=Get-Content -LiteralPath $pidPath -Raw | ConvertFrom-Json
    $process=Get-Process -Id $record.id -ErrorAction SilentlyContinue
    if (!$process) { return $null }
    if ($process.Path -ne $nodePath -or $process.StartTime.ToUniversalTime().Ticks.ToString() -ne $record.startTicks) { throw 'PID does not belong to this frontend.' }
    return $process
}
if($Action -eq 'Stop') {
    $process=Get-Frontend
    if($process) { Stop-Process -Id $process.Id; $process.WaitForExit(15000) | Out-Null }
    if(Test-Path -LiteralPath $pidPath){Remove-Item -LiteralPath $pidPath}
    Write-Output 'Local frontend stopped.'; exit 0
}
if($Action -eq 'Status') {
    $process=Get-Frontend
    if(!$process){Write-Output 'Local frontend is stopped.'; exit 0}
    $response=Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:4200/api/product' -TimeoutSec 10
    Write-Output "Frontend PID $($process.Id); proxied product API HTTP $($response.StatusCode)"
    Write-Output $response.Content; exit 0
}
if(!(Test-Path -LiteralPath $nodePath)){throw 'Run .\setup-local.ps1 first.'}
New-Item -ItemType Directory -Path $localPath -Force | Out-Null
$saved=@{}
$names=@('PATH','NODE_OPTIONS','CHROME_BIN','npm_config_cache','npm_config_userconfig','npm_config_globalconfig')
foreach($name in $names){$saved[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
try {
    $env:PATH=$nodeDir+';'+$env:PATH
    $env:NODE_OPTIONS=''
    $env:npm_config_cache=Join-Path $repoPath '.tools\npm-cache'
    $env:npm_config_userconfig=Join-Path $PSScriptRoot 'local.npmrc'
    $env:npm_config_globalconfig=Join-Path $PSScriptRoot 'local.npmrc'
    Push-Location -LiteralPath $PSScriptRoot
    try {
        if($Action -eq 'Install') {
            if(Get-Frontend){throw 'Stop the frontend before reinstalling dependencies.'}
            & $nodePath (Join-Path $nodeDir 'node_modules\npm\bin\npm-cli.js') ci --no-audit
        } elseif($Action -eq 'Start') {
            if(Get-Frontend){throw 'The frontend is already running.'}
            $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,4200)
            try{$listener.Start()}finally{$listener.Stop()}
            $process=Start-Process -FilePath $nodePath -ArgumentList @('node_modules/@angular/cli/bin/ng','serve','--host','127.0.0.1','--port','4200','--proxy-config','proxy.local.json') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localPath 'frontend.stdout.log') -RedirectStandardError (Join-Path $localPath 'frontend.stderr.log')
            @{id=$process.Id;startTicks=$process.StartTime.ToUniversalTime().Ticks.ToString()} | ConvertTo-Json | Set-Content -LiteralPath $pidPath
            $ready=$false
            for($attempt=0;$attempt -lt 60;$attempt++) {
                $process.Refresh(); if($process.HasExited){throw 'Frontend exited. Read ..\.local\frontend.stderr.log.'}
                try{$response=Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:4200' -TimeoutSec 1; if($response.StatusCode -eq 200){$ready=$true;break}}catch{Start-Sleep -Milliseconds 500}
            }
            if(!$ready){throw 'Frontend still starting. Check the logs or run Stop.'}
            Write-Output "Frontend PID $($process.Id): http://127.0.0.1:4200"; exit 0
        } elseif($Action -eq 'Build') {
            & $nodePath node_modules/@angular/cli/bin/ng build --aot
        } elseif($Action -eq 'Test') {
            if(!$env:CHROME_BIN) {
                foreach($candidate in @("$env:ProgramFiles\Google\Chrome\Application\chrome.exe","${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe")) {
                    if(Test-Path -LiteralPath $candidate){$env:CHROME_BIN=$candidate;break}
                }
            }
            & $nodePath node_modules/@angular/cli/bin/ng test --watch=false --browsers=ChromeHeadless
        } else { & $nodePath node_modules/@angular/cli/bin/ng lint }
        if($LASTEXITCODE -ne 0){throw "$Action failed with exit code $LASTEXITCODE"}
    } finally {Pop-Location}
} finally {foreach($name in $names){[Environment]::SetEnvironmentVariable($name,$saved[$name],'Process')}}
