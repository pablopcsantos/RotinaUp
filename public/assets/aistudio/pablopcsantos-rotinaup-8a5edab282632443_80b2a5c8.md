# Relatório Detalhado de Auditoria Técnica e Soluções — RotinaUp

**Repositório Analisado:** `pablopcsantos-rotinaup`[span_0](start_span)[span_0](end_span)
**Tecnologias Identificadas:** Kotlin, Jetpack Compose, Room Database (SQLite), MVVM, AppWidgetProvider[span_1](start_span)[span_1](end_span)
**Objetivo:** Identificação exaustiva de vulnerabilidades, falhas arquiteturais e de regras de negócio, acompanhada de plano de mitigação[span_2](start_span)[span_2](end_span).

Este relatório aprofundado foi gerado a partir da análise estática do código-fonte e dos arquivos de auditoria prévios presentes no repositório[span_3](start_span)[span_3](end_span).

---

## 1. Segurança e Proteção de Dados (Nível Crítico / Alto)

### 1.1. Risco de Bypass de Autenticação no Startup (P0)
*   **Problema:** O aplicativo gerencia o acesso através do estado `AuthState` (`Loading`, `Locked`, `Unlocked`) no `MainViewModel`[span_4](start_span)[span_4](end_span). Como as configurações (via `UserSettings`) são carregadas do Room de forma assíncrona, há o risco da interface (UI) resolver um estado de desbloqueio provisório antes do carregamento completo do PIN do banco de dados, permitindo acesso indevido[span_5](start_span)[span_5](end_span).
*   **Solução:** 
    *   Bloquear rigorosamente a navegação ou renderização de qualquer tela protegida (como `TodayScreen` ou `WeeklyReportScreen`) enquanto o `AuthState` estiver como `Loading`[span_6](start_span)[span_6](end_span).
    *   Utilizar uma "Splash Screen" ou tela de transição vinculada diretamente ao término do carregamento do fluxo (`StateFlow`) de configurações no `MainViewModel`[span_7](start_span)[span_7](end_span).

### 1.2. Armazenamento Legado de PIN e Criptografia (P1)
*   **Problema:** O arquivo `UserSettings.kt` ainda mantém a propriedade `pinCode: String?` em texto simples para fins de retrocompatibilidade[span_8](start_span)[span_8](end_span). Além disso, o arquivo `SecurityUtils.kt` prevê validações usando `SHA-256`, que é um algoritmo rápido e suscetível a ataques de força bruta offline (ataques de dicionário) para senhas de baixa entropia (PINs numéricos curtos)[span_9](start_span)[span_9](end_span).
*   **Solução:**
    *   Manter e forçar a migração de todas as senhas para a implementação moderna já presente no código: `PBKDF2WithHmacSHA256` com 10.000 iterações e salt criptográfico de 16 bytes[span_10](start_span)[span_10](end_span).
    *   A função `verifyPin` (em `SecurityUtils` e chamada pelo `MainViewModel`) deve obrigatoriamente forçar a limpeza (`null`) do campo `pinCode` (texto simples) do armazenamento permanente assim que o usuário legar autenticar com sucesso[span_11](start_span)[span_11](end_span).

### 1.3. Gestão Volátil do Bloqueio por Força Bruta (P1)
*   **Problema:** Embora o arquivo `SecurityUtils.kt` persista o limite de tentativas e o tempo de bloqueio usando `SharedPreferences` (`rotinaup_security_prefs`), a sincronização deste bloqueio com a interface na tela `AuthPinLockScreen.kt` pode sofrer dessincronização se basear-se puramente em estados em memória (`remember`)[span_12](start_span)[span_12](end_span). Se o processo for encerrado e reaberto, o usuário poderia tentar contornar a penalidade dependendo de como o temporizador (`LaunchedEffect`) reage[span_13](start_span)[span_13](end_span).
*   **Solução:** 
    *   Garantir que a UI sempre leia a penalização atual (`lockoutSeconds`) diretamente das `SharedPreferences` na sua inicialização, calculando a diferença entre `System.currentTimeMillis()` e o `PREF_LOCKOUT_UNTIL`[span_14](start_span)[span_14](end_span).

---

## 2. Lógica de Negócio e Relatórios (Nível Alto / Médio)

### 2.1. Inconsistência nas Fórmulas do Relatório Semanal (P1)
*   **Problema:** Na função `computeWeeklyReport` localizada no `MainViewModel.kt`, o cálculo do `overallAdherence` apresenta falhas conceituais[span_15](start_span)[span_15](end_span). O uso da função `.coerceIn(0, 100)` mascara cálculos que ultrapassem 100% (o que aponta erro no denominador)[span_16](start_span)[span_16](end_span). Além disso, os relatórios de sono calculam médias usando registros e, em ausência deles, podiam injetar valores fictícios em iterações anteriores[span_17](start_span)[span_17](end_span). A variável `bedtimeVar` utiliza desvio padrão em minutos, mas requer tratamento seguro para evitar `NaN` caso hajam poucos registros ou vazios[span_18](start_span)[span_18](end_span).
*   **Solução:**
    *   **Denominador Elegível:** Contabilizar no `totalScheduledChecks` estritamente os dias que (A) o hábito estava ativo/programado e (B) a data de verificação for igual ou posterior a `habit.createdAt`[span_19](start_span)[span_19](end_span).
    *   **Numerador Fidedigno:** Não somar marcadores `DONE` registrados por falha humana em dias em que o hábito não estava programado para ocorrer[span_20](start_span)[span_20](end_span).
    *   **Ausência de Dados:** Valores estatísticos (média de sono) devem retornar explicitamente `null` quando não houver registros suficientes (mínimo de 2 dias), evitando distorções nas "Medalhas" semanais[span_21](start_span)[span_21](end_span).

### 2.2. Reinserção de Dados Demonstrativos (Seed) (P2)
*   **Problema:** O método `seedInitialData()` injeta 6 hábitos falsos e 30 dias de logs caso interprete que o banco está vazio[span_22](start_span)[span_22](end_span). Se o usuário deletar todos os seus hábitos voluntariamente, o código (`if (current == null || !current.hasSeededInitialData)`) só previne a reinserção porque a flag `hasSeededInitialData` foi introduzida no banco de dados na versão 6 (`MIGRATION_6_7`)[span_23](start_span)[span_23](end_span).
*   **Solução:** Assegurar que nenhuma exclusão manual feita pelo usuário altere a flag `hasSeededInitialData` de volta para `false`[span_24](start_span)[span_24](end_span). O botão de "Limpar conteúdo de teste" (`clearDummyContent`) deve apagar as tabelas mas manter `hasSeededInitialData = true` e `isDummyActive = false`[span_25](start_span)[span_25](end_span).

---

## 3. Arquitetura, Banco de Dados e Background (Nível Médio)

### 3.1. Risco de Perda de Dados em Migrações do Room Database (P1/P2)
*   **Problema:** O App utiliza a classe `AppDatabase` na versão 7 com migrações explícitas (`MIGRATION_1_2` a `MIGRATION_6_7`)[span_26](start_span)[span_26](end_span). Historicamente no projeto, se houvesse a diretiva `fallbackToDestructiveMigration()`, usuários pulando versões do app perderiam todo o seu histórico (dados seriam dropados)[span_27](start_span)[span_27](end_span). 
*   **Solução:** Manter estritamente `exportSchema = true` para rastreamento de esquemas (já configurado parcialmente) e garantir testes automatizados (em `DatabaseOperationsTest.kt`) para cada salto de versão do banco, atestando que nenhuma coluna seja perdida[span_28](start_span)[span_28](end_span).

### 3.2. Lembretes e Background Acoplados à View (P1)
*   **Problema:** Verificações de alarmes em atraso (`OverdueHabitsBanner`, etc.) são impulsionadas pelo ciclo de vida da interface, usando blocos `LaunchedEffect` e rotinas de repetição (como `delay(15000)`)[span_29](start_span)[span_29](end_span). Notificações importantes falharão se o app estiver morto (fechado) ou minimizado por muito tempo[span_30](start_span)[span_30](end_span).
*   **Solução:** Substituir as chamadas temporais visuais por agendamentos nativos via `WorkManager` (para tarefas periódicas de sincronização e avisos adiáveis) ou `AlarmManager` (para despertadores e hábitos pontuais), utilizando a classe de suporte que parece existir (`AlarmScheduler` e `AlarmReceiver`)[span_31](start_span)[span_31](end_span).

### 3.3. Estagnação da Data de "Hoje" (Meia-noite) (P2)
*   **Problema:** A propriedade `_currentDateStr` e `todayDateStr` são fixadas no `MainViewModel.kt` no momento da inicialização[span_32](start_span)[span_32](end_span). Se o usuário deixar o aplicativo em memória do dia 20 para o dia 21, as marcações e telas continuarão computando logs para o dia anterior, poluindo a consistência dos dados[span_33](start_span)[span_33](end_span).
*   **Solução:** O método `updateCurrentDate()` no Viewmodel (que lê `DateUtils.getTodayDateString(0)`) deve ser engatilhado pelo `onResume()` da `MainActivity` e/ou por um observador de broadcast de mudança de data do sistema (`ACTION_TIME_TICK` ou `ACTION_DATE_CHANGED`)[span_34](start_span)[span_34](end_span).

### 3.4. Execução Assíncrona e Ocultação de Erros no Widget (P2)
*   **Problema:** O `RotinaUpWidgetProvider.kt` utiliza `CoroutineScope(Dispatchers.IO).launch` de maneira perigosa dentro do callback `onUpdate`[span_35](start_span)[span_35](end_span). Embora utilize `goAsync()` para prolongar a vida do processo, falhas de I/O caem num bloco `catch (t: Throwable)` onde apenas realizam o log silencioso ou exibem texto de erro na interface do widget, sem diagnóstico efetivo ou recuperação sistemática[span_36](start_span)[span_36](end_span).
*   **Solução:** Garantir a finalização limpa de `pendingResult.finish()` via blocos `finally` (que já existe no código, mas as exceções internas são engolidas)[span_37](start_span)[span_37](end_span). Incorporar um botão de `Sync` ou auto-retry agendado se as consultas das tabelas falharem[span_38](start_span)[span_38](end_span).

---

## 4. Qualidade de Código, CI/CD e Testes (Nível Médio)

### 4.1. Duplicação de Responsabilidades (ViewModels)
*   **Problema:** Observou-se a coexistência do `MainViewModel.kt` e do `HabitoViewModel.kt`[span_39](start_span)[span_39](end_span). O `HabitoViewModel` e o `MainViewModel` duplicam rotinas de injeção de repositório, exclusão e verificação de hábitos[span_40](start_span)[span_40](end_span).
*   **Solução:** Centralizar as regras de domínio (`UseCases`) em uma camada única de repositório e remover o ViewModel redundante, deixando a camada de visualização apenas responsável pela conversão de Estado (UI State)[span_41](start_span)[span_41](end_span).

### 4.2. Testes Automatizados Ineficazes
*   **Problema:** O arquivo `ExampleUnitTest.kt` contém apenas testes genéricos (`2 + 2 == 4`)[span_42](start_span)[span_42](end_span). Falta cobertura expressiva para fluxos cruciais[span_43](start_span)[span_43](end_span). Apesar de existir o `SecurityUtilsTest.kt` (cobrindo hashes e rate limits) e o `DatabaseOperationsTest.kt`, não existem testes cobrindo a complexa geração do `computeWeeklyReport` e travessia de meia-noite[span_44](start_span)[span_44](end_span).
*   **Solução:** Desenvolver testes focados em: (1) Mudança de datas/Fuso Horário; (2) Fórmulas matemáticas do Relatório Semanal (com listas parciais, vazias e dados inválidos); (3) Fluxo transacional de deleção em cascata[span_45](start_span)[span_45](end_span).

### 4.3. Dependências de Build e Integração Contínua
*   **Problema:** O `build.gradle.kts` configura o Build Type `release` exigindo chaves do ambiente via `System.getenv("KEYSTORE_PATH")` e outras propriedades como `STORE_PASSWORD`[span_46](start_span)[span_46](end_span). Sem documentação clara ou um workflow robusto (o arquivo `.github/workflows/android.yml` está na estrutura mas deve ser verificado), o projeto não compilará em novas máquinas[span_47](start_span)[span_47](end_span).
*   **Solução:** Documentar o fluxo de CI/CD num `README.md` detalhado (que está listado na raiz mas não possui as instruções vitais) com passos explícitos sobre como preencher o `.env` e as chaves de deploy[span_48](start_span)[span_48](end_span).

---

## Plano de Ação Estratégico (Prioridade de Execução)

1.  **Imediato (P0/P1):** Finalizar a limpeza do `pinCode` (texto puro) e implementar o bloqueio rigoroso (Loading screen) na inicialização do `MainViewModel`[span_49](start_span)[span_49](end_span). Refatorar os cálculos de adesão em `computeWeeklyReport` garantindo datas estritas[span_50](start_span)[span_50](end_span).
2.  **Curto Prazo (P1/P2):** Transferir todos os timers de lembretes da UI (`LaunchedEffect`) para `WorkManager` e certificar-se que os Widgets estão rodando atualizações com `goAsync()` totalmente seguro a falhas[span_51](start_span)[span_51](end_span).
3.  **Médio Prazo (P2):** Ampliar a cobertura de testes no Gradle para as regras de negócio de datas e relatórios; Documentar configuração `.env` no `README.md` e fundir os ViewModels redundantes[span_52](start_span)[span_52](end_span).
