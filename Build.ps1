$ErrorActionPreference = 'Stop'
$env:DOTNET_CLI_TELEMETRY_OPTOUT = '1'
$buildOut = Join-Path $PSScriptRoot 'dist'
dotnet publish (Join-Path $PSScriptRoot 'windows\PocketPad.csproj') -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:DebugType=None -p:DebugSymbols=false "-p:PathMap=$PSScriptRoot=/src" -o (Join-Path $buildOut 'PocketPad-Windows')
if ($LASTEXITCODE -ne 0) { throw 'Windows build failed.' }
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
if (!$env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
Push-Location (Join-Path $PSScriptRoot 'android')
try { & .\gradlew.bat assembleDebug testDebugUnitTest --no-daemon --console=plain; if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' } } finally { Pop-Location }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'android\app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $buildOut 'PocketPad-Android.apk')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'Start-USB.ps1') -Destination $buildOut
Write-Host "Built files: $buildOut"
