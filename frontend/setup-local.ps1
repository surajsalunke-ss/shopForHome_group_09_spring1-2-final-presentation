$ErrorActionPreference = 'Stop'
$toolsPath = Join-Path (Split-Path -Parent $PSScriptRoot) '.tools'
$nodePath = Join-Path $toolsPath 'node-v10.24.1-win-x64'
if (!(Test-Path -LiteralPath (Join-Path $nodePath 'node.exe'))) {
    New-Item -ItemType Directory -Path $toolsPath -Force | Out-Null
    $archive = Join-Path $toolsPath 'node-v10.24.1-win-x64.zip'
    if (!(Test-Path -LiteralPath $archive)) {
        & curl.exe --fail --location --silent --show-error --retry 2 'https://nodejs.org/dist/v10.24.1/node-v10.24.1-win-x64.zip' --output $archive
        if ($LASTEXITCODE -ne 0) { throw 'Node download failed.' }
    }
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne 'ae0af1b5e0c131dd0df1b3e4713c36e5d7f652ab6ca273ce46d39d4df8522bb0') { throw 'Node checksum mismatch.' }
    Expand-Archive -LiteralPath $archive -DestinationPath $toolsPath
}
& (Join-Path $nodePath 'node.exe') --version
Write-Output 'Node 10.24.1 / npm 6.14.12 are ready. Run .\local.ps1 Install, then Build and Start.'
