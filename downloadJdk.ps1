Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-EnvValueFromFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [Parameter(Mandatory = $true)]
        [string]$Key
    )

    if (-not (Test-Path $FilePath)) {
        throw "Missing .env file at '$FilePath'."
    }

    $lines = Get-Content -Path $FilePath
    foreach ($rawLine in $lines) {
        $line = $rawLine.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#")) {
            continue
        }
        $delimiterIndex = $line.IndexOf("=")
        if ($delimiterIndex -le 0) {
            continue
        }
        $k = $line.Substring(0, $delimiterIndex).Trim()
        $v = $line.Substring($delimiterIndex + 1).Trim()
        if ($k -eq $Key) {
            if (($v.StartsWith('"') -and $v.EndsWith('"')) -or ($v.StartsWith("'") -and $v.EndsWith("'"))) {
                return $v.Substring(1, $v.Length - 2)
            }
            return $v
        }
    }

    throw "Missing $Key in .env file."
}

function Remove-DirectorySafe {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (Test-Path $Path) {
        Remove-Item -Path $Path -Recurse -Force
    }
}

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$envPath = Join-Path $projectDir ".env"
$jdkUrl = Get-EnvValueFromFile -FilePath $envPath -Key "JDK_URL"

if ([string]::IsNullOrWhiteSpace($jdkUrl)) {
    throw "JDK_URL is empty in .env."
}

$jdkDir = Join-Path $projectDir ".jdk"
$tempDir = Join-Path $projectDir ".jdk-temp"
$zipPath = Join-Path $tempDir "jdk.zip"

Remove-DirectorySafe -Path $tempDir
New-Item -ItemType Directory -Path $tempDir | Out-Null

Write-Host "Downloading JDK from $jdkUrl"
Invoke-WebRequest -Uri $jdkUrl -OutFile $zipPath

if (-not (Test-Path $zipPath)) {
    throw "Download failed. Missing $zipPath."
}

Write-Host "Extracting JDK archive"
Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force

$extractedJdkDir = Get-ChildItem -Path $tempDir -Directory | Where-Object {
    $_.Name -like "jdk-25*" -and (Test-Path (Join-Path $_.FullName "bin\java.exe"))
} | Select-Object -First 1

if (-not $extractedJdkDir) {
    $available = (Get-ChildItem -Path $tempDir -Directory | Select-Object -ExpandProperty Name) -join ", "
    throw "Could not find extracted JDK folder in $tempDir. Found: $available"
}

Write-Host "Installing JDK to $jdkDir"
Remove-DirectorySafe -Path $jdkDir
Copy-Item -Path $extractedJdkDir.FullName -Destination $jdkDir -Recurse

Remove-DirectorySafe -Path $tempDir

Write-Host "Done. JDK installed to $jdkDir"
