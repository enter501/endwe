$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot
& .\gradlew.bat clean testDebugUnitTest assembleRelease
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$sourceApk = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release.apk"
$namedApk = Join-Path $PSScriptRoot "app\build\outputs\apk\release\Endwe-v1.0.0-release.apk"
Copy-Item -LiteralPath $sourceApk -Destination $namedApk -Force
Write-Host "Release APK: $namedApk"
exit 0
