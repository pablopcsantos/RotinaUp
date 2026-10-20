# RotinaUp — Assistente Pessoal de Rotina & Hábitos

RotinaUp é um aplicativo Android nativo construído com **Kotlin**, **Jetpack Compose** e **Room Database**, focado em consistência de hábitos, higiene do sono, combate à procrastinação e rotina saudável com funcionamento **100% offline**.

---

## 🚀 Arquitetura & Tecnologias

- **Linguagem:** Kotlin 2.2+
- **Interface:** Jetpack Compose com Material Design 3 (M3)
- **Persistência Local:** Room (SQLite) com migrações explícitas e transações atômicas
- **Padrão Arquitetural:** MVVM (Model-View-ViewModel) com Coroutines e StateFlow reativo
- **Widget do Sistema:** `AppWidgetProvider` nativo com `goAsync()` para ciclo de vida confiável e matriz visual de 7 dias
- **Segurança Local:** Derivação PBKDF2 com Salt criptográfico, comparação em tempo constante e bloqueio persistente contra força bruta

---

## 🛡️ Segurança e Privacidade

- **100% Offline:** Não há envio de telemetria ou dados pessoais para servidores externos. Todos os hábitos, avaliações diárias e registros de sono permanecem exclusivamente no dispositivo.
- **Proteção por PIN com PBKDF2:** Utiliza derivação lenta de chave via `PBKDF2WithHmacSHA256` (10.000 iterações, 256 bits e salt de 16 bytes), prevenindo ataques de dicionário offline caso os dados sejam inspecionados.
- **Comparação em Tempo Constante:** Previne ataques de canal lateral baseados em temporização (`MessageDigest.isEqual`).
- **Prevenção de Bypass na Inicialização:** O estado de autenticação (`AuthState`) é controlado centralmente no `MainViewModel`. O aplicativo nunca expõe interfaces protegidas enquanto as configurações do Room ainda estão sendo carregadas da mídia local.
- **Bloqueio Persistente Contra Força Bruta:** O contador de tentativas incorretas e o tempo de penalização (15s a 30s) persistem mesmo se o processo do aplicativo for forçado a fechar ou o dispositivo for reiniciado.
- **Política de Backup Controlada:** As regras em `backup_rules.xml` e `data_extraction_rules.xml` incluem o banco de rotina para preservação pessoal e excluem expressamente arquivos de contadores de tentativas de segurança.

---

## 🛠️ Compilação e Execução

### Requisitos
- **JDK:** 21
- **Android SDK:** Compile SDK 36, Target SDK 36, Min SDK 24
- **Gradle:** 9.3+

### Variáveis de Ambiente & Segredos (.env)
O projeto utiliza o Secrets Gradle Plugin para injeção segura de propriedades.
Copie o arquivo `.env.example` para `.env` caso deseje configurar propriedades locais:
```properties
# .env
GEMINI_API_KEY=sua_chave_aqui
```

### Comandos de Build
```bash
# Compilar versão Debug
gradle assembleDebug

# Executar testes unitários e testes locais Robolectric
gradle :app:testDebugUnitTest

# Gerar pacote de Release
# Requer as variáveis de ambiente de assinatura configuradas:
export KEYSTORE_PATH="/caminho/para/my-upload-key.jks"
export STORE_PASSWORD="senha_do_keystore"
export KEY_PASSWORD="senha_da_chave"
gradle assembleRelease
```

### Integração Contínua (CI/CD)
O repositório possui automação com GitHub Actions em `.github/workflows/android.yml`:
- Executa compilação limpa do projeto com JDK 21.
- Roda toda a suíte de testes unitários e testes Robolectric (`gradle :app:testDebugUnitTest`).
- Valida o schema exportado do Room Database (`exportSchema = true`) gerado via KSP.
- Para releases automatizadas no GitHub Actions, cadastre as secrets: `KEYSTORE_BASE64`, `STORE_PASSWORD` e `KEY_PASSWORD`.

---

## 📊 Métricas e Relatório Semanal
- **Adesão Geral Elegível (%):** Dias anteriores à criação de um hábito não penalizam o usuário como dias perdidos. Apenas dias programados e existentes na vida do hábito compõem o denominador e o numerador.
- **Padrões de Sono:** A média semanal e a variação horária de deitar refletem exclusivamente registros reais inseridos pelo usuário, sem substituição por médias fictícias.
- **Transações Atômicas:** Exclusões de hábitos realizam a limpeza atômica dos logs associados para evitar registros órfãos.
- **Garantia contra Reaparecimento Demonstrativo:** A inicialização de hábitos de exemplo só ocorre na primeira instalação (`hasSeededInitialData`). Excluir todos os hábitos mantém a base limpa de forma duradoura.
