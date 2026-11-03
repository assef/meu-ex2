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

## Instalação

Há dois cenários:

### A) Primeiro install, ou **após um reset de fábrica** → desbloqueio por pendrive

Depois de um reset, a central **fecha o ADB**. O único caminho é um **OTA por pendrive**
(menu secreto → atualização por USB) que abre o ADB **e** instala o app de uma vez —
sem instalar o Sapo. Passo a passo completo em **[install/DESBLOQUEIO.md](install/DESBLOQUEIO.md)**.

> Crédito do método ao **Ravi / [@appsdecarro](https://www.instagram.com/appsdecarro)**
> (ver [Créditos](#créditos)).

### B) Atualizar / reinstalar (ADB já aberto) → por Wi-Fi

Com o ADB já aberto (depois do desbloqueio), a central expõe o `adbd` na porta **5555
sem autenticação** e aceita o app assinado com a *platform key* pública do AOSP. Então
é só instalar por Wi-Fi:

1. Instale o **Android platform-tools** (`adb`) no computador.
2. Central e computador na **mesma rede Wi-Fi**.
3. Rode:

**macOS / Linux**
```sh
cd install
./install.sh                 # procura a central na rede
./install.sh 192.168.1.123   # ...ou informando o IP
```

**Windows** (não testado pelo autor — relatos bem-vindos)
```powershell
cd install
.\install.ps1
.\install.ps1 192.168.1.123
```

O script conecta via `adb`, instala o `dist/meuex2.apk` e abre o app.

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

## Créditos

Este projeto não existiria sem o trabalho de quem veio antes:

- **Ravi — [@appsdecarro](https://www.instagram.com/appsdecarro)** (projeto **Sapo** /
  EX Auto, [`swimapps/CentralEXAuto`](https://github.com/swimapps/CentralEXAuto)).
  Usei o app dele como **referência principal**: o mecanismo de acesso via `adbd`
  local, o controle dos parâmetros do carro e a ativação do CarPlay. Valeu, Ravi! 🙏
- **[`rddyas002/drive_assist`](https://github.com/rddyas002/drive_assist)** — projeto
  open-source pro próprio Geely EX2 / Geometry E. Usei como referência (telemetria
  ABRP) e os **stubs de compilação do `android.car`** (`app/car-stubs/`, Apache-2.0)
  vêm de lá.

Projetos consultados na pesquisa de CarPlay sem fio (não usados no código):
[CatPlay](https://github.com/catplay-labs/catplay) e
[ludwig-v/wireless-carplay-dongle-reverse-engineering](https://github.com/ludwig-v/wireless-carplay-dongle-reverse-engineering).

## Versões

Veja [CHANGELOG.md](CHANGELOG.md). A **v3.1** é a primeira versão pública (o
histórico anterior foi desenvolvimento privado).

## Licença

[MIT](LICENSE).
