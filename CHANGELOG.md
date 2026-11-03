# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## [3.1] — 2026-11-03 — primeira versão pública

Primeira publicação. O histórico anterior (v1–v3.0) foi desenvolvimento privado,
então **todas as funções abaixo são novas** para o público.

### Condução
- Modo de condução: **Eco / Conforto / Sport**.
- Regeneração (freio motor): **Baixa / Média / Alta**.
- **Lembrar ao ligar** (por função): reaplica sua escolha a cada partida.

### Segurança
- **Frenagem automática (AEB)**: liga/desliga, com opção de manter ao ligar.
- **Silenciar AVAS** (alerta sonoro de pedestre), reversível.

### Carro
- **Janela acompanha a porta**: abre ~10% ao abrir a porta e fecha ao fechar;
  opção "só a janela oposta".
- **Câmera ao engatar o D** (AVM).
- **Atalho de Wi-Fi**.

### Telemetria
- **Envio ao ABRP** (A Better Routeplanner) via API genérica: SoC, velocidade e
  GPS (GPS opcional). Configurável no app (token + API key).

### Sistema
- **Reiniciar a central** (com confirmação, via modal no estilo do app).

### Infra / build
- App de sistema assinado com a AOSP platform test-key pública.
- Stubs de compilação do `android.car` (sem precisar extrair o jar do firmware).
- Scripts de instalação por ADB sem fio para **macOS/Linux** e **Windows**.
- APK pré-compilado em `dist/`.

### Notas
- **CarPlay sem fio foi removido/deferido**: no IHU629G depende de um hack instável
  do AP + um bug conhecido do iOS. Para sem fio estável, use um dongle dedicado
  (ex.: Carlinkit) no CarPlay com fio. No dia a dia, use **CarPlay por cabo**.
