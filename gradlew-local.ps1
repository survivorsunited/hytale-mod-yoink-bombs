Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Wrapper to run Gradle with a repo-local JDK.
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkDir = Join-Path $projectDir ".jdk"
$javaExe = Join-Path $jdkDir "bin\java.exe"

if (-not (Test-Path $javaExe)) {
    Write-Error "JDK not found at '$jdkDir'. Place a JDK 25 under .jdk so '$javaExe' exists."
}

$env:JAVA_HOME = $jdkDir
$env:Path = "$($env:JAVA_HOME)\bin;$($env:Path)"

& (Join-Path $projectDir "gradlew.bat") @args
