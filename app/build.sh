#!/usr/bin/env bash
# Builds meuex2.apk from source, signed with the AOSP platform test-key.
# Requires: JDK 17, Android SDK build-tools 34.0.0 (aapt2, d8, zipalign, apksigner),
#           and the android-28 platform (android.jar).
#
# Point ANDROID_SDK to your SDK root (or set ANDROID_HOME / ANDROID_SDK_ROOT).
# JDK 17: set JAVA_HOME, or it will try `java` from PATH.
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_SDK="${ANDROID_SDK:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}"
if [ -z "$ANDROID_SDK" ]; then
  echo "ERRO: defina ANDROID_SDK (ou ANDROID_HOME) apontando pro Android SDK." >&2
  exit 1
fi
BT_VER="${BUILD_TOOLS_VERSION:-34.0.0}"
BT="$ANDROID_SDK/build-tools/$BT_VER"
AJ="$ANDROID_SDK/platforms/android-28/android.jar"
CARJAR="${CARJAR:-car-stubs/car-stubs.jar}"
KEY="${KEYSTORE_DIR:-keystore}"

[ -x "$BT/aapt2" ] || { echo "ERRO: build-tools $BT_VER não encontrado em $BT" >&2; exit 1; }
[ -f "$AJ" ] || { echo "ERRO: android-28 platform (android.jar) não encontrado em $AJ" >&2; exit 1; }

# JDK 17
if [ -n "${JAVA_HOME:-}" ]; then JAVAC="$JAVA_HOME/bin/javac"; else JAVAC="javac"; fi

rm -rf obj classes.dex *.apk compiled_res.zip base.apk unsigned.apk aligned.apk; mkdir -p obj
# recursos (ícones etc.) -> flat zip
"$BT/aapt2" compile --dir res -o compiled_res.zip
# link: manifest + recursos
"$BT/aapt2" link -o base.apk -I "$AJ" --manifest AndroidManifest.xml \
  -R compiled_res.zip --min-sdk-version 28 --target-sdk-version 28 --auto-add-overlay
"$JAVAC" --release 17 -classpath "$AJ:$CARJAR" -d obj $(find src -name '*.java')
"$BT/d8" --min-api 28 --lib "$AJ" --output . $(find obj -name '*.class')
cp base.apk unsigned.apk && zip -qj unsigned.apk classes.dex
"$BT/zipalign" -f 4 unsigned.apk aligned.apk
"$BT/apksigner" sign --key "$KEY/platform.pk8" --cert "$KEY/platform.x509.pem" --out meuex2.apk aligned.apk
rm -f base.apk unsigned.apk aligned.apk classes.dex compiled_res.zip
echo "OK -> meuex2.apk ($(wc -c < meuex2.apk) bytes)"
