#!/usr/bin/env bash
# Prepara um pendrive FAT32 com o pacote de desbloqueio do Meu EX2 na raiz.
# (macOS / Linux.) Baseado no preparador do Ravi / @appsdecarro — crédito a ele.
#
# Pré-requisito: rode antes o tools/build-unlock.sh, que gera dist/unlock/.
# Uso: ./preparar_pendrive.sh
set -euo pipefail
cd "$(dirname "$0")"

SRC="../dist/unlock"
[ -d "$SRC" ] || { echo "ERRO: $SRC não existe. Rode tools/build-unlock.sh primeiro." >&2; exit 1; }

echo "ATENÇÃO: isto vai COPIAR o desbloqueio para um pendrive. Use um pendrive FAT32"
echo "(formate antes se precisar). Confirme bem qual volume é o pendrive!"
echo

OS="$(uname -s)"
if [ "$OS" = "Darwin" ]; then
  echo "Volumes montados:"; ls -1 /Volumes
  read -p "Nome do volume do pendrive (em /Volumes/...): " V
  DST="/Volumes/$V"
else
  echo "Pontos de montagem (procure seu pendrive):"; lsblk -o NAME,SIZE,FSTYPE,MOUNTPOINT 2>/dev/null || mount
  read -p "Caminho de montagem do pendrive (ex: /media/voce/PENDRIVE): " DST
fi
[ -d "$DST" ] || { echo "ERRO: $DST não é uma pasta válida." >&2; exit 1; }

read -p "Copiar o desbloqueio para '$DST'? (s/n): " ok
[ "$ok" = "s" ] || { echo "Cancelado."; exit 1; }

cp -Rv "$SRC"/* "$DST"/
sync
echo
echo "OK! Pendrive pronto. Agora siga install/DESBLOQUEIO.md (menu secreto + atualização por USB)."
