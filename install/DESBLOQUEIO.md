# Primeiro install (pós-reset): desbloqueio + app

Depois de um **reset de fábrica** a central fecha o ADB (o desbloqueio "morre"), então
não dá pra instalar nada por Wi-Fi. O único caminho é um **OTA por pendrive** aplicado
pelo recovery (menu secreto → atualização por USB). Nosso pacote abre o ADB **e** já
instala o app — sem instalar o Sapo.

> **Crédito:** todo o método (menu secreto, OTA por USB, o truque de abrir o ADB via
> `persist.adb.tcp.port`) é descoberta do **Ravi — [@appsdecarro](https://www.instagram.com/appsdecarro)**
> (projeto Sapo / EX Auto). Nosso pacote só reusa o `update-binary` stock do **seu**
> pacote oficial dele e troca o payload pelo nosso app. Valeu, Ravi! 🙏

## ⚠️ Risco

- O OTA só mexe em `/data` (nunca `/system`) e termina sem apagar nada
  (`intentional_no_wipe`). **`ERRO 7` é esperado** — não se preocupe.
- Se o pacote for **rejeitado** pelo recovery (assinatura), nada acontece.
- Se o **preload do app** falhar, o pior caso é o app não aparecer no boot — aí é só
  instalar por Wi-Fi com `install/install.sh` (o ADB já estará aberto).
- Um **reset de fábrica reverte tudo.** Use por sua conta e risco (garantia etc.).

## 1. Gerar o pacote

Você precisa de:
- **`signapk.jar`** (ferramenta de OTA do AOSP). Baixe de fonte confiável
  (ex.: AOSP `prebuilts/sdk/tools/lib/signapk.jar`).
- **O seu pacote oficial de desbloqueio do Ravi** (`arquivo_sapo.zip`) — usado só pra
  extrair o `update-binary` stock do recovery (não redistribuímos nada dele).
- O APK do app (`dist/meuex2.apk`, ou rode `app/build.sh`).

```sh
cd tools
RAVI_ZIP=/caminho/arquivo_sapo.zip \
SIGNAPK=/caminho/signapk.jar \
APK=../dist/meuex2.apk \
./build-unlock.sh
```

Isso gera `dist/unlock/` (pendrive pronto) e `dist/meuex2-unlock-update.zip`.

> **Nome da pasta do pendrive**: `3C6025_SW0E22H`. O updater da central casa por
> `startsWith("3C6025_" + 7 primeiros chars da versão instalada)`, então o mesmo
> pacote serve pras versões 1111 e 1114. Ajuste `FOLDER=` no build se a sua for outra.

## 2. Preparar o pendrive (FAT32)

```sh
# macOS/Linux
cd install && ./preparar_pendrive.sh
```
```powershell
# Windows
cd install; .\preparar_pendrive.ps1
```

O conteúdo de `dist/unlock/` (a pasta `3C6025_SW0E22H/OS/update.zip`) vai pra **raiz**
do pendrive.

## 3. Aplicar na central (menu secreto)

1. Plugue o pendrive na central.
2. Abra o **app de Telefone** (sem estar conectado no Bluetooth) e digite o código do
   menu secreto:

   **`#*` + (mês + 10) + dia + hora**

   - **mês + 10**: setembro (09) → `19`
   - **dia**: dia do mês (2 dígitos)
   - **hora**: hora atual no **formato 12h** (2 dígitos)

   Exemplo: 11/set às 14h → `#*191102` (19, dia 11, 14h = 02 no formato 12h).
3. No menu secreto, toque no botão que começa com **"U"** (atualização por USB) —
   é o **último botão da quarta linha**.
4. Escolha o pacote do pendrive e confirme. **`ERRO 7` é esperado.** Ao terminar, **reinicie**.

## 4. Pronto

No boot, o ADB sobe na porta 5555 e o app já aparece. Para **atualizar** depois (sem
pendrive), use o Wi-Fi:

```sh
cd install && ./install.sh         # ou ./install.sh <ip-da-central>
```
