# RotinaUp — Assistente Pessoal de Rotina & Hábitos

RotinaUp é um aplicativo Android nativo construído com **Kotlin**, **Jetpack Compose** e **Room Database**, focado em consistência de hábitos, higiene do sono, combate à procrastinação e rotina saudável com funcionamento **100% offline**.

---

## 🚀 Arquitetura & Tecnologias

- **Linguagem:** Kotlin 2.2+
- **Interface:** Jetpack Compose com Material Design 3 (M3)
- **Persistência Local:** Room (SQLite) com migrações explícitas e transações consistentes
- **Padrão Arquitetural:** MVVM (Model-View-ViewModel) com Coroutines e StateFlow reativo
- **Widget do Sistema:** `AppWidgetProvider` nativo com matriz visual de 7 dias
- **Segurança Local:** Proteção por PIN com derivação criptográfica (Salt + SHA-256) e limitação de tentativas

---

## 🛡️ Segurança e Privacidade

- **100% Offline:** Não há envio de telemetria ou dados pessoais para servidores externos. Todos os hábitos, avaliações diárias e registros de sono permanecem exclusivamente no dispositivo.
- **Armazenamento Seguro de PIN:** O código de bloqueio não é armazenado em texto claro. Utiliza-se um salt aleatório criptográfico (16 bytes) e hash SHA-256.
- **Proteção Contra Força Bruta:** O componente `AuthPinLockScreen` implementa bloqueio progressivo temporário após tentativas consecutivas incorretas.
- **Integridade dos Dados:** Migrações Room explícitas garantem preservação contínua do banco de dados local entre atualizações.

---

## 🛠️ Compilação e Execução

### Requisitos
- **JDK:** 21
- **Android SDK:** Compile SDK 36, Target SDK 36, Min SDK 24
- **Gradle:** 9.3+

### Comandos de Build
```bash
# Compilar versão Debug
gradle assembleDebug

# Executar testes unitários e testes locais Robolectric
gradle :app:testDebugUnitTest

# Gerar pacote de Release (requer variáveis de keystore)
gradle assembleRelease
```

### Configuração de Variáveis de Ambiente & Secrets
O projeto utiliza o Secrets Gradle Plugin configurado para ler do arquivo `.env` (com modelo em `.env.example`).
Nenhuma chave sensível deve ser comitada no repositório.

### Assinatura de Release
A configuração de release em `app/build.gradle.kts` utiliza:
- `KEYSTORE_PATH` (padrão: `${rootDir}/my-upload-key.jks`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- Alias fixo: `upload`
Em ambiente de CI/CD, forneça estas variáveis via secrets seguros do pipeline.

---

## 📊 Métricas e Relatório Semanal
- **Adesão Geral (%):** Calculada estritamente sobre hábitos ativos e os dias efetivamente programados na janela dos últimos 7 dias.
- **Padrões de Sono:** A média semanal e a variação horária de deitar (desvio padrão) refletem exclusivamente registros reais inseridos pelo usuário.
- **Transações:** Exclusões de hábitos realizam a limpeza atômica dos logs associados para evitar registros órfãos.
