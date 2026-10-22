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
- **Proteção por PIN com PBKDF2:** Utiliza `PBKDF2WithHmacSHA256` com 100.000 iterações por padrão e chave derivada de 256 bits. O salt é gerado com 16 bytes aleatórios criptograficamente seguros e armazenado em formato hexadecimal.
- **Comparação em Tempo Constante:** Previne ataques de canal lateral baseados em temporização (`MessageDigest.isEqual`).
- **Prevenção de Bypass na Inicialização:** O estado de autenticação (`AuthState`) é controlado centralmente no `MainViewModel`. O aplicativo nunca expõe interfaces protegidas enquanto as configurações do Room ainda estão sendo carregadas da mídia local.
- **Bloqueio Persistente Contra Força Bruta:** O contador de tentativas incorretas e os prazos de bloqueio persistem entre reinicializações do processo e do dispositivo. A penalização cresce após falhas consecutivas: 15 s, 30 s, 60 s e 300 s nas tentativas seguintes.
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
./gradlew assembleDebug

# Executar testes unitários e testes locais Robolectric
./gradlew :app:testDebugUnitTest

# Gerar pacote de Release
# Requer as variáveis de ambiente de assinatura configuradas:
export KEYSTORE_PATH="/caminho/para/my-upload-key.jks"
export STORE_PASSWORD="senha_do_keystore"
export KEY_PASSWORD="senha_da_chave"
./gradlew assembleRelease
```

### Integração Contínua (CI/CD)
O repositório possui automação com GitHub Actions em `.github/workflows/android.yml`:
- Executa compilação limpa do projeto com JDK 21.
- Executa a suíte de testes unitários e Robolectric (`./gradlew :app:testDebugUnitTest`).
- A compilação usa KSP para gerar o schema exportado do Room (`exportSchema = true`); o workflow atual não executa uma etapa independente de comparação/validação do schema.
- O workflow atual executa testes e compila o APK Debug; ele não publica releases nem compila a variante Release. Para compilar Release localmente com assinatura, configure `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD` como variáveis de ambiente; `KEY_ALIAS` é opcional (padrão: `upload`). Sem um keystore válido, a variante Release pode ser compilada sem assinatura.

---

## 📊 Métricas e Relatório Semanal
- **Adesão Geral Elegível (%):** Dias anteriores à criação de um hábito não penalizam o usuário como dias perdidos. Apenas dias programados e existentes na vida do hábito compõem o denominador e o numerador.
- **Padrões de Sono:** A média semanal e a variação horária de deitar refletem exclusivamente registros reais inseridos pelo usuário, sem substituição por médias fictícias.
- **Transações Atômicas:** Exclusões de hábitos realizam a limpeza atômica dos logs associados para evitar registros órfãos.
- **Garantia contra Reaparecimento Demonstrativo:** A inicialização de hábitos de exemplo só ocorre na primeira instalação (`hasSeededInitialData`). Excluir todos os hábitos mantém a base limpa de forma duradoura.
