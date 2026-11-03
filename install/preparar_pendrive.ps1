# Prepara um pendrive FAT32 com o pacote de desbloqueio do Meu EX2 na raiz. (Windows.)
# Baseado no preparador do Ravi / @appsdecarro — crédito a ele.
# Nao testado pelo autor no Windows — relatos sao bem-vindos.
#
# Pre-requisito: rode antes o tools/build-unlock.sh (em WSL/git-bash), que gera dist/unlock/.
# Uso: .\preparar_pendrive.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$Src = "..\dist\unlock"
if (-not (Test-Path $Src)) { throw "$Src nao existe. Rode tools/build-unlock.sh primeiro." }

Write-Host "ATENCAO: isto vai COPIAR o desbloqueio para um pendrive FAT32."
Write-Host "Confirme bem qual drive e o pendrive!`n"

Get-Volume | Where-Object { $_.DriveLetter -and $_.DriveType -eq 'Removable' } |
  Format-Table DriveLetter, FileSystemLabel, FileSystem, @{N='Size(GB)';E={[math]::Round($_.Size/1GB,1)}}

$letter = Read-Host "Letra do drive do pendrive (ex: E)"
$dst = "${letter}:\"
if (-not (Test-Path $dst)) { throw "$dst nao existe." }

$ok = Read-Host "Copiar o desbloqueio para '$dst'? (s/n)"
if ($ok -ne "s") { Write-Host "Cancelado."; exit 1 }

Copy-Item -Recurse -Force "$Src\*" $dst
Write-Host "`nOK! Pendrive pronto. Agora siga install/DESBLOQUEIO.md (menu secreto + atualizacao por USB)."
