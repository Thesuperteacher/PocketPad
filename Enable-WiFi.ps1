#Requires -RunAsAdministrator
param([switch]$Remove)
$ErrorActionPreference='Stop'
$ruleName='PocketPad-Private-Local-19876'
if($Remove){Get-NetFirewallRule -Name $ruleName -ErrorAction SilentlyContinue | Remove-NetFirewallRule; Write-Host 'PocketPad firewall rule removed.'; exit}
$program=Join-Path $PSScriptRoot 'PocketPad-Windows\PocketPad.exe'
if(!(Test-Path -LiteralPath $program)){throw 'Keep this helper next to the PocketPad-Windows folder.'}
$program=(Resolve-Path -LiteralPath $program).Path
$existing=Get-NetFirewallRule -Name $ruleName -ErrorAction SilentlyContinue
if($existing){throw 'PocketPad rule already exists. Use -Remove before creating it for a different app location.'}
New-NetFirewallRule -Name $ruleName -DisplayName 'PocketPad - private local network only' -Direction Inbound -Action Allow -Program $program -Protocol TCP -LocalPort 19876 -Profile Private -RemoteAddress LocalSubnet | Out-Null
Write-Host 'PocketPad is allowed on private local networks only. No public-network or router rule was added.'
