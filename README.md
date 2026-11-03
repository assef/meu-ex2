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

Depois de um reset, a central **fecha o ADB**, então não dá pra instalar por Wi-Fi. O
único caminho é um **OTA por pendrive**, aplicado pelo recovery via o **menu secreto →
atualização por USB**. O nosso pacote **abre o ADB e instala o app de uma vez — sem Sapo.**

> **Crédito do método** (menu secreto, OTA por USB, truque de abrir o ADB) ao
> **Ravi / [@appsdecarro](https://www.instagram.com/appsdecarro)** (projeto Sapo / EX Auto).
> Nosso pacote só reusa o `update-binary` stock e troca o payload pelo nosso app. 🙏

#### ⚠️ Antes de começar
- Só mexe em `/data` (nunca `/system`), termina sem apagar nada, e **`ERRO 7` é esperado**.
- Pacote rejeitado pela assinatura = nada acontece. Preload do app falhar = instale por
  Wi-Fi depois (passo B). **Um reset de fábrica reverte tudo.** Por sua conta e risco.

#### 1. Pegue o pacote de desbloqueio
- **Pronto**: baixe o `meuex2-unlock-update.zip` da [última release](https://github.com/assef/meu-ex2/releases/latest)
  (serve se o seu firmware casar com `3C6025_SW0E22H` — cobre as versões 1111 e 1114).
- **Ou gere o seu** (ver [Compilar do código](#compilar-do-código) + `tools/build-unlock.sh`).

#### 2. Prepare um pendrive FAT32
Coloque o pacote na **raiz** do pendrive, nesta estrutura exata:
```
PENDRIVE/
└── 3C6025_SW0E22H/
    └── OS/
        └── update.zip      ← o meuex2-unlock-update.zip, renomeado para update.zip
```
Ou use o script (gera a estrutura e copia pra você):
```sh
cd install && ./preparar_pendrive.sh          # macOS/Linux
#   (Windows: .\preparar_pendrive.ps1)
```

#### 3. Plugue o pendrive e abra o menu secreto da central
1. Plugue o pendrive numa porta USB da central.
2. Abra o app de **Telefone (dialer)** da central — **sem** estar conectado a nenhum
   Bluetooth.
3. Digite o código do menu secreto:

   **`#*` + (mês + 10) + dia + hora**

   | parte | como calcular | exemplo (11/set, 14h) |
   |---|---|---|
   | mês + 10 | mês com 2 dígitos **+ 10** | set = 09 → **19** |
   | dia | dia do mês, 2 dígitos | **11** |
   | hora | hora atual no **formato 12h**, 2 dígitos | 14h → **02** |

   → código do exemplo: **`#*191102`**

#### 4. Atualização por USB
1. No menu secreto, toque no botão que começa com **"U"** (atualização por USB) —
   é o **último botão da quarta linha**.
2. Selecione o pacote do pendrive e confirme.
3. **`ERRO 7` é esperado, não se preocupe.** (Dica do método: pise no freio e coloque em P.)
4. Ao terminar, **reinicie a central**.

#### 5. Pronto
No boot, o ADB sobe (porta 5555) e o app **Meu EX2** já aparece. Daí em diante, pra
**atualizar** é só Wi-Fi (passo B abaixo).

> Detalhes técnicos e geração do pacote: **[install/DESBLOQUEIO.md](install/DESBLOQUEIO.md)**.

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
