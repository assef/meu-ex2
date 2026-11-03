#!/usr/bin/env bash
# Instala o Meu EX2 na central Geely EX2 / Geometry E (IHU629G) via ADB sem fio.
#
# A central expõe o adbd na porta 5555 SEM autenticação (é assim que o app roda
# como system). Este script conecta, instala o APK assinado com a platform key e
# inicia o app. Use após uma revisão/reset de fábrica pra reinstalar.
#
# Uso:
#   ./install.sh                 # procura a central na sua rede local
#   ./install.sh 192.168.1.123   # usa o IP informado (pula a busca)
#   APK=/caminho/meuex2.apk ./install.sh 192.168.1.123
#
# Pré-requisitos: adb no PATH (Android platform-tools). Central e computador na
# MESMA rede Wi-Fi.
set -euo pipefail
cd "$(dirname "$0")"

APK="${APK:-../dist/meuex2.apk}"
PORT=5555
PKG=com.geely.meuex2

command -v adb >/dev/null 2>&1 || { echo "ERRO: 'adb' não encontrado. Instale o Android platform-tools." >&2; exit 1; }
[ -f "$APK" ] || { echo "ERRO: APK não encontrado em $APK" >&2; exit 1; }

is_headunit() { # $1 = ip:porta  -> 0 se parece ser a central
  adb -s "$1" shell pm list packages 2>/dev/null | grep -qiE 'ecarx|njda|com\.ex\.auto' && return 0
  adb -s "$1" shell getprop ro.product.brand 2>/dev/null | grep -qiE 'ecarx|geely|geometry' && return 0
  return 1
}

TARGET=""
if [ "${1:-}" != "" ]; then
  TARGET="$1:$PORT"
  adb connect "$TARGET" >/dev/null 2>&1 || true
else
  echo "Procurando a central na rede local (porta $PORT)..."
  # descobre a sua sub-rede /24
  if command -v ip >/dev/null 2>&1; then
    BASE=$(ip -4 addr show scope global 2>/dev/null | grep -oE 'inet [0-9]+\.[0-9]+\.[0-9]+' | head -1 | awk '{print $2}')
  else
    BASE=$(ifconfig 2>/dev/null | grep -oE 'inet [0-9]+\.[0-9]+\.[0-9]+' | grep -v '127.0.0' | head -1 | awk '{print $2}')
  fi
  [ -n "$BASE" ] || { echo "ERRO: não achei sua rede. Passe o IP: ./install.sh <ip>" >&2; exit 1; }
  for i in $(seq 1 254); do
    ip="$BASE.$i"
    ( (command -v nc >/dev/null 2>&1 && nc -z -G 1 "$ip" $PORT 2>/dev/null) && echo "$ip" ) &
  done > /tmp/_ex2_hits 2>/dev/null; wait 2>/dev/null || true
  for ip in $(sort -u /tmp/_ex2_hits 2>/dev/null); do
    adb connect "$ip:$PORT" >/dev/null 2>&1 || true
    if is_headunit "$ip:$PORT"; then TARGET="$ip:$PORT"; break; fi
  done
  rm -f /tmp/_ex2_hits
  [ -n "$TARGET" ] || { echo "ERRO: central não encontrada. Confirme que está ligada, na mesma Wi-Fi, e passe o IP: ./install.sh <ip>" >&2; exit 1; }
fi

echo "Central: $TARGET"
adb -s "$TARGET" get-state >/dev/null 2>&1 || { echo "ERRO: ADB não conectou em $TARGET." >&2; exit 1; }
echo "Modelo:  $(adb -s "$TARGET" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"

echo "Instalando $APK ..."
adb -s "$TARGET" install -r "$APK"
echo "Iniciando o app..."
adb -s "$TARGET" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
echo "OK. Abra 'Meu EX2' na central e confira."
