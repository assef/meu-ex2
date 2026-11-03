#!/usr/bin/env bash
# Gera o pacote de DESBLOQUEIO do Meu EX2: um update.zip (OTA) que, aplicado pelo
# recovery da central (menu secreto -> atualização por USB):
#   1) abre o ADB   -> adiciona persist.adb.tcp.port=5555 ao persistent_properties
#   2) pré-instala  -> nosso app (com.geely.meuex2) como app de sistema
#
# NÃO instala o Sapo nem redistribui nada de terceiros. O método (menu secreto +
# OTA + o truque da property) é descoberta do **Ravi / @appsdecarro** (projeto
# Sapo / EX Auto) — crédito total a ele. Reaproveitamos apenas o `update-binary`
# (interpretador edify STOCK do AOSP) do SEU próprio pacote oficial de desbloqueio,
# que você baixou do Ravi — ou seja, você fornece o zip base; nós só trocamos o
# payload pelo nosso app e re-assinamos.
#
# Uso:
#   RAVI_ZIP=/caminho/arquivo_sapo.zip \
#   SIGNAPK=/caminho/signapk.jar \
#   APK=../dist/meuex2.apk \
#   ./build-unlock.sh
#
# Saída: dist/unlock/ (pendrive pronto, pasta 3C6025_.../OS/update.zip)
#        e dist/meuex2-unlock-update.zip (o OTA assinado).
#
# Requisitos: java (JDK 17), zip/unzip, o signapk.jar do AOSP e o SEU zip oficial
# de desbloqueio do Ravi (só pra extrair o update-binary stock).
set -euo pipefail
cd "$(dirname "$0")"

RAVI_ZIP="${RAVI_ZIP:-}"
SIGNAPK="${SIGNAPK:-}"
APK="${APK:-../dist/meuex2.apk}"
KEYDIR="${KEYSTORE_DIR:-../app/keystore}"
PKG="com.geely.meuex2"
CODEPATH="/data/app/$PKG-1"
# Nome da pasta do pendrive: 3C6025_ + 7 primeiros chars da versão do seu firmware.
# O updater da central casa por startsWith, então SW0E22H serve pra 1111 e 1114.
FOLDER="${FOLDER:-3C6025_SW0E22H}"
JAVA="${JAVA:-java}"

[ -n "$RAVI_ZIP" ] && [ -f "$RAVI_ZIP" ] || { echo "ERRO: defina RAVI_ZIP com o seu zip oficial de desbloqueio do Ravi (p/ extrair o update-binary stock)." >&2; exit 1; }
[ -n "$SIGNAPK" ] && [ -f "$SIGNAPK" ] || { echo "ERRO: defina SIGNAPK com o caminho do signapk.jar (AOSP)." >&2; exit 1; }
[ -f "$APK" ] || { echo "ERRO: APK não encontrado em $APK (rode app/build.sh antes)." >&2; exit 1; }
[ -f "$KEYDIR/platform.pk8" ] && [ -f "$KEYDIR/platform.x509.pem" ] || { echo "ERRO: chave não encontrada em $KEYDIR." >&2; exit 1; }

WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
EX="$WORK/ravi"; mkdir -p "$EX"
unzip -q "$RAVI_ZIP" -d "$EX"
# acha o update-binary STOCK dentro do pacote do Ravi (pode estar em um update.zip interno)
UB="$(find "$EX" -path '*META-INF/com/google/android/update-binary' | head -1)"
if [ -z "$UB" ]; then
  INNER="$(find "$EX" -name 'update.zip' | head -1)"
  [ -n "$INNER" ] || { echo "ERRO: não achei update-binary nem update.zip dentro do RAVI_ZIP." >&2; exit 1; }
  unzip -q "$INNER" "META-INF/com/google/android/update-binary" -d "$EX/inner"
  UB="$EX/inner/META-INF/com/google/android/update-binary"
fi
[ -f "$UB" ] || { echo "ERRO: update-binary stock não encontrado." >&2; exit 1; }

# monta a árvore do nosso OTA
Z="$WORK/zip"; mkdir -p "$Z/META-INF/com/google/android" "$Z/data/app/$PKG-1" "$Z/data/local/tmp"
cp "$UB" "$Z/META-INF/com/google/android/update-binary"
cp "$APK" "$Z/data/app/$PKG-1/base.apk"

# pkg_entry — registra o app como sistema (sharedUserId=1000) no packages.xml
TS="1a02619c838"
cat > "$Z/data/local/tmp/$PKG.pkg_entry" <<EOF
    <package name="$PKG" codePath="$CODEPATH" nativeLibraryPath="$CODEPATH/lib" publicFlags="0" privateFlags="0" ft="$TS" it="$TS" ut="$TS" version="1" sharedUserId="1000">
        <perms />
    </package>
EOF

# pkgdb.sh — preload do pacote (baseado no cex_pkgdb.sh do Ravi, adaptado p/ o nosso)
cat > "$Z/data/local/tmp/pkgdb.sh" <<EOF
#!/system/bin/sh
# Preload $PKG — adaptado do cex_pkgdb.sh do Ravi (@appsdecarro). Crédito a ele.
d=$CODEPATH
x=/data/system/packages.xml
f=/data/local/tmp/$PKG.pkg_entry
[ -f "\$d/base.apk" ] || { echo '[MEUEX2] APK nao extraido' >&2; exit 1; }
chmod 0644 "\$d/base.apk"; mkdir -p "\$d/lib"; chmod 0755 "\$d" "\$d/lib"; chown -R 1000:1000 "\$d"
restorecon -RF "\$d" 2>/dev/null || chcon -R u:object_r:apk_data_file:s0 "\$d"
[ -f "\$x" ] || { echo '[MEUEX2] packages.xml ausente' >&2; exit 1; }
if grep -q 'name="$PKG"' "\$x"; then
  grep -q 'codePath="'"\$d"'"' "\$x" && echo '[MEUEX2] ja registrado - mantendo' >&2 || { echo '[MEUEX2] outro codePath - removendo preload' >&2; rm -rf "\$d"; }
  rm -f "\$f"; exit 0
fi
[ -f "\$f" ] || { echo '[MEUEX2] pkg_entry ausente' >&2; exit 1; }
cp -f "\$x" "\$x.bak"
awk -v f="\$f" 'BEGIN{ins=0}
  ins==0 && /^[[:space:]]*<(updated-package|shared-user|keyset-settings)/ { while ((getline l < f) > 0) print l; ins=1 }
  {print}
  END{ if (ins==0) exit 9 }' "\$x" > "\$x.new" \
  && tail -c 12 "\$x.new" | grep -q '</packages>' \
  && grep -q 'name="$PKG"' "\$x.new" \
  && mv -f "\$x.new" "\$x" && chmod 660 "\$x" && chown 1000:1000 "\$x" \
  && { restorecon "\$x" 2>/dev/null || true; } && echo '[MEUEX2] packages.xml OK' >&2 \
  || { echo '[MEUEX2] packages.xml FALHOU - restaurando' >&2; rm -f "\$x.new"; cp -f "\$x.bak" "\$x"; }
rm -f "\$f"; sync
EOF

# updater-script — abre o adb + instala nosso app (baseado no do Ravi, creditado)
cat > "$Z/META-INF/com/google/android/updater-script" <<EOF
ui_print("=== Meu EX2 - desbloqueio (adb) + instalacao ===");
ui_print("=== Metodo: Ravi / @appsdecarro (projeto Sapo). Credito a ele. ===");
ui_print("=== ERRO 7 ESPERADO, NAO SE PREOCUPE. Pise no freio e coloque em P. ===");
getprop("ro.product.device") == "IHU629G" || abort("Wrong device: expected IHU629G");
mount("ext4", "EMMC", "/dev/block/platform/bootdevice/by-name/system", "/system");
mount("ext4", "EMMC", "/dev/block/platform/bootdevice/by-name/userdata", "/data");
set_progress(0.3);
run_program("/system/bin/sh", "-c", "f=/data/property/persistent_properties; [ -f \"\$f\" ] || exit 0; if ! strings \"\$f\" 2>/dev/null | grep -q 'persist.adb.tcp.port'; then { printf '\\\\012\\\\034\\\\012\\\\024persist.adb.tcp.port\\\\022\\\\004'; printf '5555'; } >> \"\$f\"; echo '[ADB] port 5555 adicionada' >&2; else echo '[ADB] ja presente' >&2; fi; restorecon -v \"\$f\" 2>/dev/null || true");
set_progress(0.7);
run_program("/system/bin/sh", "-c", "rm -rf $CODEPATH; mkdir -p $CODEPATH/lib");
package_extract_file("data/app/$PKG-1/base.apk", "$CODEPATH/base.apk");
package_extract_file("data/local/tmp/$PKG.pkg_entry", "/data/local/tmp/$PKG.pkg_entry");
package_extract_file("data/local/tmp/pkgdb.sh", "/data/local/tmp/pkgdb.sh");
run_program("/system/bin/sh", "/data/local/tmp/pkgdb.sh");
run_program("/system/bin/sh", "-c", "rm -f /data/local/tmp/pkgdb.sh");
ui_print("=== FINALIZADO. Pode reiniciar. ===");
abort("intentional_no_wipe");
EOF

# empacota (update-binary primeiro ajuda alguns recoveries) e assina (whole-file, -w)
UNS="$WORK/unsigned.zip"; OUT_ZIP="dist/meuex2-unlock-update.zip"
mkdir -p dist
( cd "$Z" && zip -qrX "$UNS" META-INF/com/google/android/update-binary \
    META-INF/com/google/android/updater-script data )
"$JAVA" -jar "$SIGNAPK" -w "$KEYDIR/platform.x509.pem" "$KEYDIR/platform.pk8" "$UNS" "$OUT_ZIP"
echo "OTA assinado -> $OUT_ZIP ($(wc -c < "$OUT_ZIP") bytes)"

# monta a pasta do pendrive
PD="dist/unlock"; rm -rf "$PD"; mkdir -p "$PD/$FOLDER/OS"
cp "$OUT_ZIP" "$PD/$FOLDER/OS/update.zip"
echo "Pendrive pronto em: $PD/  (copie o conteúdo para a RAIZ de um pendrive FAT32)"
echo "Estrutura: $FOLDER/OS/update.zip"
