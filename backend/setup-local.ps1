$ErrorActionPreference = 'Stop'
$toolsPath = Join-Path (Split-Path -Parent $PSScriptRoot) '.tools'
New-Item -ItemType Directory -Path $toolsPath -Force | Out-Null
$packages = @(
    @{
        directory = 'jdk-11.0.32.1+1'; archive = 'jdk11.zip'; algorithm = 'SHA256'
        url = 'https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.32.1%2B1/OpenJDK11U-jdk_x64_windows_hotspot_11.0.32.1_1.zip'
        hash = 'd5008f02174c1ad21c10407cbf104815cb390ec4c263d53946e8d0507d7a77f9'
    },
    @{
        directory = 'apache-maven-3.9.16'; archive = 'maven.zip'; algorithm = 'SHA512'
        url = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip'
        hash = 'ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3'
    }
)
foreach ($package in $packages) {
    $destination = Join-Path $toolsPath $package.directory
    if (Test-Path -LiteralPath $destination) {
        Write-Output "Already installed: $destination"
        continue
    }
    $archivePath = Join-Path $toolsPath $package.archive
    if (!(Test-Path -LiteralPath $archivePath)) {
        & curl.exe --fail --location --silent --show-error --retry 2 --output $archivePath $package.url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $($package.directory)" }
    }
    if ((Get-FileHash -LiteralPath $archivePath -Algorithm $package.algorithm).Hash -ne $package.hash) {
        throw "Checksum mismatch: $archivePath"
    }
    Expand-Archive -LiteralPath $archivePath -DestinationPath $toolsPath
    Write-Output "Installed: $destination"
}
Write-Output 'Project-local tools are ready. Run .\local.ps1 Build, then .\local.ps1 Start.'
