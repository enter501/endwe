$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot
& .\gradlew.bat clean testDebugUnitTest assembleDebug
exit $LASTEXITCODE
