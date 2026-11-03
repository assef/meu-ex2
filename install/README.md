# Instalação

Scripts para instalar o Meu EX2 na central via ADB sem fio.

- `install.sh` — macOS / Linux
- `install.ps1` — Windows (não testado pelo autor)

## Pré-requisitos

1. **Android platform-tools** (`adb`) no `PATH`.
   - macOS: `brew install android-platform-tools`
   - Linux: pacote `android-tools-adb` (ou baixe do site do Android)
   - Windows: baixe o platform-tools e adicione a pasta ao `PATH`
2. **Central e computador na mesma rede Wi-Fi.**

## Uso

```sh
# macOS/Linux — procura a central sozinho:
./install.sh
# ...ou informando o IP da central (mais rápido e confiável):
./install.sh 192.168.1.123
```

```powershell
# Windows:
.\install.ps1
.\install.ps1 192.168.1.123
```

## Como achar o IP da central

- Na central: **Configurações → Wi-Fi** → toque na rede conectada → veja o IP.
- Ou no painel do seu roteador, procure o dispositivo da central.

## Por que funciona sem senha de ADB

O firmware do IHU629G deixa o `adbd` escutando na porta **5555 sem autenticação**.
O script só conecta e instala o APK (assinado com a platform key que o ROM aceita).
Depois de um reset de fábrica, o `adbd` volta aberto — então é só rodar de novo.

## Instalar um APK específico

```sh
APK=/caminho/para/meuex2.apk ./install.sh 192.168.1.123
```
