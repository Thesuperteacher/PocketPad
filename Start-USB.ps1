param([string]$DeviceSerial, [switch]$Disconnect)
$ErrorActionPreference = 'Stop'
$adbCandidates = @((Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'), (Join-Path $PSScriptRoot 'platform-tools\adb.exe'))
$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$adbPath = if ($adbCommand) { $adbCommand.Source } else { $adbCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1 }
if (!$adbPath) { throw 'Android Platform Tools are required for this USB debugging option. USB tethering also works and does not need these tools. See README.md.' }
if (!$DeviceSerial) {
    $devices = @(& $adbPath devices | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
    if ($devices.Count -ne 1) { throw 'Connect exactly one phone by USB and approve its USB debugging prompt, or supply -DeviceSerial.' }
    $DeviceSerial = $devices[0]
}
if ($Disconnect) { & $adbPath -s $DeviceSerial reverse --remove tcp:19876; if ($LASTEXITCODE -ne 0) { throw 'Could not remove the USB forwarding rule.' }; Write-Host 'PocketPad USB forwarding removed.'; exit }
& $adbPath -s $DeviceSerial reverse tcp:19876 tcp:19876
if ($LASTEXITCODE -ne 0) { throw 'Could not establish the USB forwarding rule.' }
Write-Host 'USB is ready. In the PC companion choose USB cable / this PC only, then Start connection.'
Write-Host 'On the phone choose Connect > Scan PC code > USB debugging cable.'
Write-Host 'When finished: run this helper with -Disconnect. You can also turn off USB debugging.'
