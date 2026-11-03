# Meu EX2

App de sistema (home/ajustes) para a central **Geely EX2 / Geometry E (IHU629G, Android 9)**.
Substitui o que eu usava de apps de terceiros por um app próprio, enxuto, só com as
funções que eu uso de verdade — reversível e estável.

> *System app for the Geely EX2 / Geometry E head unit (IHU629G). Portuguese UI.
> Source-available; see [Contribuindo](#contribuindo).*

---

## ⚠️ Aviso

Projeto pessoal, feito pro **meu** carro. Mexe em parâmetros do veículo e usa o
acesso `adb`/root que o firmware já expõe. **Use por sua conta e risco.** Nada é
gravado em `/system` (dm-verity); tudo vive em `/data` + ajustes de runtime, então
**um reset de fábrica devolve a central ao padrão**. Ainda assim, você é
responsável pelo seu carro e pela sua garantia.

## Funções

### Condução
- **Modo de condução**: Eco / Conforto / Sport.
- **Regeneração (freio motor)**: Baixa / Média / Alta.
- **Lembrar ao ligar**: reaplica sua escolha a cada partida (opcional, por função).

### Segurança
- **Frenagem automática (AEB)**: liga/desliga, com opção de **manter sua escolha ao ligar**.
- **Silenciar AVAS**: muta o alerta sonoro de pedestre (reversível).

### Carro
- **Janela acompanha a porta**: ao abrir a porta, o vidro abre um dedo (~10%) e
  fecha ao fechar a porta. Opção **"só a janela oposta"**.
- **Câmera ao engatar o D**: abre a câmera/AVM ao entrar em Drive.
- **Atalho de Wi-Fi**: abre a configuração de Wi-Fi da central.

### Telemetria (ABRP)
- Envia dados ao vivo (SoC, velocidade, GPS) para o **A Better Routeplanner**
  via API genérica (`api.iternio.com/1/tlm/send`). Configure token + API key no app.
- Toggle para **enviar/omitir o GPS**.
- Requer internet na central (ex.: hotspot do celular). Veja nota sobre CarPlay abaixo.

### Sistema
- **Reiniciar a central** (com confirmação).

> **CarPlay sem fio foi removido** (deferido). No IHU629G ele depende de um hack
> instável do AP e de um bug conhecido do iOS (convites de CarPlay ignorados após
> reconexões). Para CarPlay sem fio estável, use um **dongle dedicado** (ex.:
> Carlinkit) no CarPlay com fio do carro. No dia a dia, **CarPlay por cabo** é o
> caminho estável — e ainda dá internet pra central (útil pro ABRP).

## Instalação (APK pré-compilado)

A central expõe o `adbd` na porta **5555 sem autenticação** — é assim que o app
roda como `system` (assinado com a *platform key* pública do AOSP, que o ROM aceita).

1. Instale o **Android platform-tools** (`adb`) no computador.
2. Deixe a **central e o computador na mesma rede Wi-Fi**.
3. Rode o script:

**macOS / Linux**
```sh
cd install
./install.sh                 # procura a central na rede
# ou, se souber o IP:
./install.sh 192.168.1.123
```

**Windows** (não testado pelo autor — relatos bem-vindos)
```powershell
cd install
.\install.ps1
# ou:
.\install.ps1 192.168.1.123
```

O script conecta via `adb`, instala o `dist/meuex2.apk` e abre o app.

### Reinstalar após revisão / reset de fábrica

Nas revisões eu **reseto a central** (para evitar questões de garantia) e reinstalo
depois. É só repetir o passo de instalação acima — a central volta com o `adbd`
aberto de fábrica, então o script reinstala sem nenhum pré-passo.

## Compilar do código

Requisitos: **JDK 17**, **Android SDK build-tools 34.0.0**, **platform android-28**.

```sh
export ANDROID_SDK="$HOME/Android/sdk"   # raiz do seu SDK
export JAVA_HOME="/caminho/para/jdk-17"  # opcional, se javac não estiver no PATH
cd app
./build.sh                               # gera app/meuex2.apk
```

Detalhes:
- **Assinatura**: AOSP *platform test-key* pública (`app/keystore/`). O ROM confia
  nela, por isso o app roda como `system`. Veja `app/keystore/README.md`.
- **android.car**: usamos stubs de compilação (`app/car-stubs/`) para não precisar
  extrair o `android.car.jar` do firmware. Em runtime o Android carrega o real.
  Veja `app/car-stubs/README.md`.

## Como funciona

- **App de sistema** (`sharedUserId=android.uid.system`), então acessa as APIs do
  carro via `android.car` (`CarPropertyManager`) e permissões privilegiadas.
- **Root pontual** para reiniciar a central: o `RootShell` fala o protocolo ADB
  direto no `adbd` local (`127.0.0.1:5555`), que roda como root e sem auth neste firmware.
- **Serviços em segundo plano**: `CentralService` (porta→vidro, marcha→câmera),
  `ApplyService` (reaplica ajustes no boot), `AbrpService` (telemetria).
- Nada em `/system`. Tudo reversível por reset de fábrica.

## Contribuindo

Código **aberto para leitura e fork**. **Não aceito PRs** — eu só implemento as
funções que eu uso. Se você quer mais, **faça um fork** à vontade. Veja
[CONTRIBUTING.md](CONTRIBUTING.md).

## Licença

[MIT](LICENSE).
