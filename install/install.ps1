# Instala o Meu EX2 na central Geely EX2 / Geometry E (IHU629G) via ADB sem fio.
#
# A central expoe o adbd na porta 5555 SEM autenticacao. Este script conecta,
# instala o APK assinado com a platform key e inicia o app.
#
# Uso:
#   .\install.ps1                  # procura a central na sua rede local
#   .\install.ps1 192.168.1.123    # usa o IP informado
#   $env:APK="C:\caminho\meuex2.apk"; .\install.ps1 192.168.1.123
#
# Pre-requisitos: adb no PATH (Android platform-tools). Central e computador na
# MESMA rede Wi-Fi.  (Nao testado pelo autor no Windows — relatos sao bem-vindos.)
param([string]$Ip = "")
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$Apk  = if ($env:APK) { $env:APK } else { "..\dist\meuex2.apk" }
$Port = 5555
$Pkg  = "com.geely.meuex2"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { throw "'adb' nao encontrado. Instale o Android platform-tools." }
if (-not (Test-Path $Apk)) { throw "APK nao encontrado em $Apk" }

function Is-HeadUnit($target) {
  $pkgs = adb -s $target shell pm list packages 2>$null
  if ($pkgs -match 'ecarx|njda|com\.ex\.auto') { return $true }
  $brand = adb -s $target shell getprop ro.product.brand 2>$null
  if ($brand -match 'ecarx|geely|geometry') { return $true }
  return $false
}

$target = ""
if ($Ip -ne "") {
  $target = "${Ip}:$Port"
  adb connect $target | Out-Null
} else {
  Write-Host "Procurando a central na rede local (porta $Port)..."
  $local = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.PrefixOrigin -ne 'WellKnown' -and $_.IPAddress -notlike '127.*' } | Select-Object -First 1).IPAddress
  if (-not $local) { throw "Nao achei sua rede. Passe o IP: .\install.ps1 <ip>" }
  $base = ($local -split '\.')[0..2] -join '.'
  foreach ($i in 1..254) {
    $ip = "$base.$i"
    if ((Test-Connection -ComputerName $ip -TcpPort $Port -Quiet -TimeoutSeconds 1 -ErrorAction SilentlyContinue)) {
      adb connect "${ip}:$Port" | Out-Null
      if (Is-HeadUnit "${ip}:$Port") { $target = "${ip}:$Port"; break }
    }
  }
  if ($target -eq "") { throw "Central nao encontrada. Ligue-a, use a mesma Wi-Fi, ou passe o IP: .\install.ps1 <ip>" }
}

Write-Host "Central: $target"
adb -s $target get-state | Out-Null
$model = (adb -s $target shell getprop ro.product.model 2>$null).Trim()
Write-Host "Modelo:  $model"

Write-Host "Instalando $Apk ..."
adb -s $target install -r $Apk
Write-Host "Iniciando o app..."
adb -s $target shell monkey -p $Pkg -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "OK. Abra 'Meu EX2' na central e confira."
