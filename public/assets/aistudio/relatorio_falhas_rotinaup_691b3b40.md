# RotinaUp — Relatório de Falhas, Riscos e Soluções

**Repositório analisado:** `pablopcsantos-rotinaup` (Android nativo · Kotlin 2.2 · Jetpack Compose · Room v7 · AGP 9.1.1)
**Data da análise:** 20/10/2026
**Método:** leitura estática de todo o código-fonte, recursos, scripts de build, CI e testes do arquivo enviado (~12,8 mil linhas). Nada foi compilado nem executado.

---

## 1. Resumo executivo

O projeto tem uma base boa: migrações explícitas do Room (sem `fallbackToDestructiveMigration`), exclusão atômica de hábito + logs, `goAsync()` nos receivers, PBKDF2 com salt e comparação em tempo constante, bloqueio de tentativas persistido. As auditorias antigas incluídas em `public/assets/aistudio/` apontavam problemas que **já foram corrigidos no código atual** (estado `Loading` da autenticação, atualização da data à meia-noite, flag de seed).

Mesmo assim, encontrei **falhas que impedem o build, enfraquecem a segurança de forma real e quebram as promessas do README** (lembretes, dados 100% reais, PIN robusto). As mais graves:

| # | Severidade | Falha |
|---|---|---|
| A1 | 🔴 Crítico | `SettingsScreen` é usada em `MainActivity` mas não existe no repositório (build quebra) |
| B1 | 🔴 Crítico | Limite de tentativas do PIN é contornável na tela de bloqueio (checagem de prefixo sem registrar falha) |
| C1 | 🟠 Alto | Primeira execução sobrescreve as flags do seed: dados falsos de 30 dias entram sem aviso |
| D1 | 🟠 Alto | `POST_NOTIFICATIONS` é declarada mas nunca solicitada (sem notificações no Android 13+) |
| D2 | 🟠 Alto | Alarmes nunca são exatos (falta permissão de alarme exato) |
| D3/D4 | 🟠 Alto | Reagendamento pós-boot não é aguardado; cadeia de lembretes se rompe em vários caminhos |
| E1 | 🟠 Alto | Flag `isFromWidget` fica "presa" e silencia todos os alertas |
| B2/B3 | 🟠 Alto | App nunca re-bloqueia; PIN protege só a interface (banco sem criptografia) |
| A2/A3 | 🟠 Alto | `gradlew`/wrapper e `debug.keystore` ausentes: CI e `assembleDebug` falham |

**Contagem:** 2 críticos · 13 altos · 22 médios · 8 baixos.

> **Legenda:** 🔴 Crítico · 🟠 Alto · 🟡 Médio · 🔵 Baixo
> Itens marcados **[verificar]** dependem de algo que não aparece no arquivo recebido (ex.: arquivos possivelmente ignorados pelo `.gitignore`).

---

## 2. Build, CI e repositório

### A1 🔴 `SettingsScreen` referenciada, mas inexistente
- **Onde:** `MainActivity.kt` (`composable("settings") { SettingsScreen(...) }`). Não há `SettingsScreen.kt` na árvore de arquivos, nem definição em nenhum outro arquivo.
- **Impacto:** `Unresolved reference: SettingsScreen` → o projeto **não compila**. Além disso, todo o fluxo que depende dessa tela (definir/remover PIN, limpar conteúdo de teste, pré-visualizar widget, disparar alarme de teste) fica sem ponto de entrada. As auditorias antigas do repositório citam `SettingsScreen.kt`, sugerindo que o arquivo existe localmente mas não foi commitado **[verificar]**.
- **Solução:** commitar `SettingsScreen.kt`; conferir `git status` e o `.gitignore` (padrões como `*Settings*` ou `*.kt` em pastas ignoradas). Rodar `git ls-files | grep Settings` para confirmar.

### A2 🟠 Gradle Wrapper ausente
- **Onde:** a árvore mostra apenas `gradle/wrapper/gradle-wrapper.properties`; faltam `gradlew`, `gradlew.bat` e `gradle-wrapper.jar`. O workflow executa `./gradlew ...` e o README manda usar `gradle ...` (Gradle do sistema).
- **Impacto:** o CI falha logo no primeiro passo; builds locais dependem da versão do Gradle instalada na máquina (README exige 9.3+).
- **Solução:** `gradle wrapper --gradle-version 9.3.1` e commitar os 4 arquivos; trocar `gradle` por `./gradlew` no README. **[verificar]** se foram ignorados pelo `.gitignore`.

### A3 🟠 `debug.keystore` referenciado e não versionado
- **Onde:** `app/build.gradle.kts` → `signingConfigs.create("debugConfig") { storeFile = file("${rootDir}/debug.keystore") ... }` aplicado ao build `debug`.
- **Impacto:** `assembleDebug` (incluindo o passo "Build Debug APK" do CI) falha com "Keystore file not found". **[verificar]**
- **Solução (melhor):** remover o `debugConfig` customizado e usar a assinatura debug padrão do AGP. Se for necessário um keystore fixo, gerá-lo no CI:
  ```yaml
  - name: Gerar debug.keystore
    run: keytool -genkeypair -v -keystore debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  ```

### A4 🟡 README promete o que o CI não faz
- O README diz que o CI "valida o schema exportado do Room" e descreve secrets de release (`KEYSTORE_BASE64`, etc.). O `android.yml` só roda `testDebugUnitTest` e `assembleDebug`: não há validação de schema, lint, build de release nem upload de artefatos.
- **Solução:** adicionar `lintDebug`, um passo que falha se `app/schemas/` mudar sem commit (`git diff --exit-code app/schemas`), e (opcional) job de release que decodifica `KEYSTORE_BASE64`. Ou corrigir o README para refletir o que existe.

### A5 🟡 Configuração de release e identidade do app
- `isMinifyEnabled = false` + `material-icons-extended` (biblioteca enorme) → APK inflado e sem ofuscação.
- `applicationId = "com.aistudio.rotinaviva.kqzmfd"` (placeholder gerado) e `namespace = "com.example"`: inadequados para publicação e difíceis de trocar depois que houver usuários.
- `signingConfigs.release` lê `STORE_PASSWORD`/`KEY_PASSWORD` de variáveis de ambiente; sem elas, `assembleRelease` falha só no fim, com erro pouco claro.
- **Solução:** definir o `applicationId` definitivo agora; habilitar `isMinifyEnabled = true` + `isShrinkResources = true` (regras Room/Compose já cobertas pelo AGP); trocar `material-icons-extended` por ícones SVG/vector próprios ou apenas os usados; validar variáveis no início do script com mensagem clara.

### A6 🟡 Contradição "100% offline" × Gemini/Secrets
- `metadata.json` declara `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`; o plugin Secrets injeta `GEMINI_API_KEY` no `BuildConfig`; o README afirma "100% offline, sem telemetria".
- **Risco:** se alguém colocar uma chave real no `.env`, ela é compilada no APK e extraível. Não vi `.gitignore` no arquivo recebido, então também não dá para garantir que `.env` e keystores não sejam commitados **[verificar]**.
- **Solução:** remover Secrets plugin, `.env.example` e a capability do `metadata.json` enquanto não houver uso de API; garantir no `.gitignore`: `.env`, `*.jks`, `*.keystore`, `local.properties`.

### A7 🟡 Dependências desatualizadas e catálogo poluído
- `composeBom = 2024.09.00`, `lifecycle 2.8.7`, `navigation 2.8.9`, `room 2.7.0` convivem com AGP 9.1.1, Kotlin 2.2.10 e compileSdk 36.1: combinação defasada, sem correções recentes.
- O `libs.versions.toml` mantém dezenas de entradas não usadas (Firebase, Retrofit, OkHttp, CameraX, Moshi, Coil, Credentials...). Parte delas aparece comentada no `build.gradle.kts`.
- `kotlinx-coroutines-core` é redundante (já vem com `-android`).
- **[verificar]** a matriz de compatibilidade **KSP × Kotlin × AGP 9** (KSP `2.3.5` com Kotlin `2.2.10`).
- **Solução:** atualizar BOM/lifecycle/navigation/room, remover entradas mortas, rodar `./gradlew dependencyUpdates` (ou Renovate/Dependabot).

### A8 🔵 Higiene do repositório
- `public/assets/aistudio/*.md` são relatórios gerados por IA com marcadores `[span_N]`, citam arquivos que não existem (`HabitoViewModel.kt`, `SettingsScreen.kt`) e apontam bugs já corrigidos. Remover ou mover para `docs/archive/`.
- Nomes duplicados: `Habito`/`Habit`/`HabitEntity`, `HabitoDao`/`HabitDao` (typealiases). Escolher um nome.
- `colors.xml` ainda com cores do template; `themes.xml` chama o tema de `Theme.MyApplication`.
- `ExampleInstrumentedTest`/`ExampleRobolectricTest` são do template.

---

## 3. Segurança

### B1 🔴 Limite de tentativas do PIN contornável
- **Onde:** `AuthPinLockScreen.kt`, função `onDigit`.
  ```kotlin
  if (next.length >= 4 && onVerifyPin(next, false)) { onSuccess() }   // recordFailure = false
  else if (next.length == 6) { submitPin(next) }                      // só aqui registra falha
  ```
- **Problema:** a cada dígito a partir do 4º, o PIN parcial é verificado **sem registrar falha**. Falha só é contada ao chegar a 6 dígitos ou tocar em "OK". Um atacante digita 4 dígitos, olha o resultado, apaga e repete — sem contagem e sem bloqueio. Para PINs de 4 ou 5 dígitos (o desenho da tela usa 4 pontos), o espaço de 10.000 combinações pode ser percorrido na UI sem nunca acionar o lockout.
- **Efeitos colaterais:** o PIN "1234" libera quem digitar `1234…` antes de completar outro PIN; até 3 execuções de PBKDF2 por toque, na thread principal.
- **Solução:**
  ```kotlin
  private const val PIN_LENGTH = 4            // ou guardar o tamanho nas configurações

  fun onDigit(d: String) {
      if (lockoutSeconds > 0 || pin.length >= PIN_LENGTH) return
      pin += d; isError = false
      if (pin.length == PIN_LENGTH) submitPin(pin)   // toda tentativa completa conta
  }
  ```
  Mover a verificação para `viewModelScope` com `Dispatchers.Default` e aplicar o lockout **dentro** de `MainViewModel.verifyPin`, não só na UI (hoje `verifyPin(…, recordFailure=false)` ignora o lockout).

### B2 🟠 O app nunca re-bloqueia
- **Onde:** `MainViewModel._isSessionUnlocked` só volta a `false` quando o processo morre.
- **Impacto:** depois de desbloquear uma vez, sair para a tela inicial e voltar (mesmo horas depois) não pede PIN novamente.
- **Solução:** observar o ciclo de vida do processo e bloquear após um tempo em segundo plano:
  ```kotlin
  ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, e ->
      if (e == Lifecycle.Event.ON_STOP) viewModel.onBackgrounded()      // grava elapsedRealtime
      if (e == Lifecycle.Event.ON_START) viewModel.lockIfTimeoutExceeded(30_000)
  })
  ```
  Aplicar também `WindowManager.LayoutParams.FLAG_SECURE` enquanto bloqueado (impede captura de tela e miniatura em "Recentes").

### B3 🟠 O PIN protege só a interface; dados e hash seguem expostos
- O banco `rotinaup.db` é SQLite em texto claro. O PIN não criptografa nada: quem tiver acesso ao arquivo (backup, root, `adb` em build debug) lê hábitos, sono e diário.
- `allowBackup=true` e as regras **incluem** o banco, que contém `pinHash` e `pinSalt`.
- PBKDF2 com **10.000 iterações** está bem abaixo da referência atual da OWASP (600.000 para PBKDF2-HMAC-SHA256). E, principalmente, um PIN de 4–6 dígitos tem 10⁴–10⁶ combinações: **nenhum KDF salva isso** num ataque offline. O README ("previne ataques de dicionário offline") exagera.
- O "salt de 16 bytes" é, na prática, uma string hex de 32 caracteres usada como bytes ASCII (`salt.toByteArray(UTF_8)`): funciona, mas não corresponde ao que o README descreve.
- **Solução:**
  1. Criptografar o banco (SQLCipher) com chave protegida pelo Android Keystore, ou ao menos excluir `user_settings`/PIN do backup.
  2. Oferecer `BiometricPrompt` (com fallback para credencial do dispositivo) como método principal; o PIN do app vira reserva.
  3. Elevar iterações (≥ 600.000) com tempo calibrado no aparelho, e gravar o número de iterações no hash (já feito) para permitir upgrade gradual.
  4. Corrigir o README para não prometer proteção que o esquema não entrega.

### B4 🟡 Lockout fraco e manipulável
- Usa `System.currentTimeMillis()`: mudar a data/hora do aparelho encerra o bloqueio.
- Máximo de **30 s** a partir da 4ª tentativa; 10.000 PINs × 30 s ≈ 3,5 dias de força bruta na UI — viável.
- `recordFailedAttempt` usa `apply()` (assíncrono): matar o processo logo após a tentativa pode perder a contagem.
- A UI mostra "4/3 tentativas" depois da 3ª falha.
- **Solução:** `SystemClock.elapsedRealtime()` (+ guardar o boot count para detectar reinicialização), backoff exponencial (15 s, 30 s, 1 min, 5 min, 30 min…), `commit()` nas gravações de segurança, e texto da UI corrigido.

### B5 🟡 Widget expõe dados com o app bloqueado
- `RotinaUpWidgetProvider` mostra nomes e horários dos hábitos na tela inicial sem considerar `requireAuth`.
- **Solução:** se o PIN estiver ativo, mostrar apenas progresso agregado (ex.: "3/5 hoje") ou oferecer a opção "ocultar nomes no widget".

### B6 🔵 Outros pontos de segurança/privacidade
- `RotinaUpWidgetProvider` é `exported=true` e aceita `com.example.ACTION_SYNC_WIDGET` de qualquer app (apenas causa atualização; baixo risco). Remover essa action do `intent-filter` e usar só `PendingIntent` interno.
- Exportação do relatório vai para a **área de transferência** com dados de saúde. No Android 13+, marcar o clip como sensível (`ClipDescription.EXTRA_IS_SENSITIVE`).
- Exportação CSV não neutraliza nomes que começam com `=`, `+`, `-`, `@` (injeção de fórmula em planilhas).
- Migração do PIN legado (`createHashedSettings`) força `requireAuth = true`, mesmo se o usuário o havia desativado.
- Comentário em `UserSettings` ainda diz "SHA-256 salted hash" (desatualizado).

---

## 4. Dados e regras de negócio

### C1 🟠 Seed sobrescreve as flags: dados falsos entram sem aviso
- **Onde:** `MainViewModel.init` + `seedInitialData()`.
  ```kotlin
  val current = settingsDao.getSettings().firstOrNull()      // null na 1ª execução
  if (current == null || !current.hasSeededInitialData) {
      seedInitialData()                                        // grava UserSettings(isDummyActive = true, hasSeenDummyPrompt = false)
      val updated = (current ?: UserSettings()).copy(hasSeededInitialData = true)
      settingsDao.insertOrUpdateSettings(updated)              // REPLACE com o valor antigo → hasSeenDummyPrompt = true
  }
  ```
- **Problema:** `current` foi lido **antes** do seed. O `REPLACE` final restaura os defaults (`hasSeenDummyPrompt = true`), então o `DummyContentDialog` (`!hasSeenDummyPrompt`) **nunca aparece**. O usuário passa a ver 6 hábitos de exemplo e 30 dias de sono/diário/cochilos inventados como se fossem seus. Relatório semanal, medalhas e tier já nascem calculados sobre dados falsos, o que contradiz o README ("registros reais inseridos pelo usuário").
- **Solução:** gravar tudo em uma única transação e não reescrever as configurações com dado velho:
  ```kotlin
  db.withTransaction {
      habitDao.insertAllHabits(...); habitDao.insertAllLogs(...); /* sleep, nap, review */
      settingsDao.insertOrUpdateSettings(
          UserSettings(isDummyActive = true, hasSeenDummyPrompt = false, hasSeededInitialData = true)
      )
  }
  ```
  Melhor ainda: **não semear dados falsos por padrão**; oferecer "carregar exemplos" como opção, ou um onboarding com hábitos sugeridos (sem logs históricos).
- Defaults de `UserSettings` incoerentes: `isDummyActive = true` e `hasSeenDummyPrompt = true` ao mesmo tempo.

### C2 🟠 `clearDummyContent()` apaga também os dados reais
- **Onde:** `MainViewModel.clearDummyContent()` executa `deleteAllLogs()`, `deleteAllSleepLogs()`, `deleteAllNapLogs()`, `deleteAllDailyReviews()` e `deleteAllAlarmEvents()`.
- **Problema:** não há marcação de "registro de exemplo". Se o botão for usado depois que o usuário começou a registrar (a tela de ajustes não está no arquivo, então não dá para ver as condições), **todo o histórico real é destruído**. Os hábitos de exemplo, por outro lado, **não** são removidos.
- **Solução:** adicionar coluna `isSample INTEGER NOT NULL DEFAULT 0` nas tabelas semeadas (migração 7→8) e remover só `WHERE isSample = 1`, incluindo os hábitos de exemplo; ou apagar apenas datas anteriores ao dia da instalação (`createdAt` do primeiro seed). Exigir confirmação em dois passos.

### C3 🟠 Horários digitados como texto livre, sem validação
- **Onde:** `HabitEditDialog` ("Horário (HH:mm)"), `SleepScreen` (deitar, acordar, cochilo). Nenhum `TimePicker`, máscara ou validação; `ifBlank { "08:00" }` é o único tratamento.
- **Consequências concretas:**
  - `MainActivity` decide atraso por **comparação de strings** (`h.targetTime <= currentTimeStr`): `"8:00"` ou `"9:30"` entram em ordem errada (`"9:30" > "10:00"`), o alerta não dispara ou dispara no momento errado. O mesmo vale para `ORDER BY targetTime` no SQL.
  - `AlarmScheduler.calculateNextTriggerTime` devolve `null` para `"abc"` (hábito sem lembrete, sem aviso ao usuário) e, para `"25:99"`, `Calendar` (leniente) rola para o dia seguinte.
  - `saveSleepLog` captura a exceção e grava **480 min (8 h) inventados**, enquanto a string inválida segue salva; o relatório depois usa `?: 23`/`?: 0` como "valor padrão". Contradiz a promessa de não substituir por médias fictícias.
  - Mesmo horário de deitar e acordar vira **24 h de sono** (`if (m <= 0) m += 1440`).
  - Duração do cochilo/hábito aceita negativos e valores absurdos.
- **Solução:** usar `TimePicker` do Material 3 e salvar como minutos do dia (`Int`) ou `LocalTime`; validar duração (ex.: sono 1–16 h, cochilo 1–240 min) e recusar salvar quando inválido, com mensagem. Migração: converter `targetTime` existente com `LocalTime.parse` e corrigir/descartar valores inválidos.

### C4 🟡 Sem chaves estrangeiras: logs órfãos
- `habit_logs.habitId` e `alarm_events.habitId` não têm `@ForeignKey`.
- `AlarmReceiver.handleQuickDone` insere log para qualquer `habitId`, inclusive de hábito já excluído (a notificação continua na bandeja após `deleteHabit`).
- `deleteAllHabits()` não limpa logs; `deleteHabit` não cancela a notificação do hábito.
- **Solução:** `@ForeignKey(entity = Habito::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = CASCADE)` em `HabitLog` (migração 7→8 recriando a tabela e removendo órfãos antes), e `NotificationManagerCompat.cancel(habit.id.toInt())` em `deleteHabit`/`toggleHabitActive`. Em `handleQuickDone`, validar que o hábito existe e está ativo.

### C5 🟡 Backup do Room em modo WAL pode perder dados
- `backup_rules.xml` e `data_extraction_rules.xml` incluem somente `rotinaup.db`. O Room usa WAL por padrão; transações recentes ficam em `rotinaup.db-wal`, que não é copiado.
- **Solução:** incluir também `rotinaup.db-wal` e `rotinaup.db-shm`, ou forçar checkpoint (`PRAGMA wal_checkpoint(TRUNCATE)`) antes do backup; validar com `adb shell bmgr` e restauração em aparelho limpo. Reavaliar se `user_settings` (com `pinHash`) deve ir para o backup (ver B3).

### C6 🟡 "Lost update" nas configurações e reagendamento desnecessário
- `updateSettings(settings.copy(...))` grava a linha inteira a partir de um snapshot da UI. Duas alterações em sequência (ex.: alternar tema enquanto outra grava) podem sobrescrever uma à outra; o mesmo vale para o upgrade de PIN em `verifyPin` (usa `current` antigo).
- Toda alteração, até trocar o tema, chama `rescheduleAllAlarms`.
- **Solução:** `@Query("UPDATE user_settings SET themeMode = :mode WHERE id = 1")` por campo (ou `@Transaction` com leitura + escrita) e reagendar só quando campos relacionados a alarme mudarem.

### C7 🟡 Seed não transacional e com IDs fixos
- 6 hábitos com `id = 1..6` e dezenas de inserts sem transação: se o processo morrer no meio, o próximo start semeia de novo (duplicando logs; `REPLACE` mascara parte do problema). Resolvido com `withTransaction` (ver C1).

### C8 🟡 Relatório semanal: inconsistências de cálculo
Em `MainViewModel.computeWeeklyReport`:
1. `WeeklyReportData.hasEnoughData` retorna `daysWithData >= 5`, enquanto internamente (sugestão e medalhas) usa `daysWithData >= 5 && activeHabits.isNotEmpty() && totalScheduledChecks >= 5`. A UI pode mostrar números enquanto o texto diz "dados insuficientes".
2. `rate = 100` quando o hábito não tem dia elegível na semana — mostra 100 % de adesão sem base. Deveria ser `null`/"N/A" e ficar fora de médias.
3. O dia de **hoje** entra no denominador mesmo antes do horário do hábito; a adesão cai durante a manhã e "se corrige" depois.
4. Medalhas dependem do **nome** do hábito (`contains("despertar")`, `"cochilo"`, `"hidrata"`): renomear/excluir o hábito de exemplo torna a medalha inalcançável; só o primeiro match conta.
5. O hábito "Não tirar cochilos…" é do tipo `FOCUS`, então suas "faltas" entram em `focusSkips` e quebram a medalha "Foco Inabalável" por motivo alheio.
6. "Transformação Verde" zera a sequência em dias não programados (hábito só de dias úteis nunca atravessa um fim de semana).
7. Exportação JSON/CSV usa `/7` fixo em "dias concluídos", ignorando os dias realmente programados.
8. `remember(habits, allLogs, …)` na tela não inclui a data: após a meia-noite o relatório e o `periodStr` ficam velhos até algum dado mudar. O cálculo percorre todo o histórico na thread principal.
- **Solução:** unificar o flag de dados suficientes; usar `Int?` para taxa; contar o dia atual só após o `targetTime` (ou separar "hoje" do cálculo); vincular medalhas a tipo/ID/tag (`habit.badgeKey`) em vez de texto; calcular sequências apenas sobre dias elegíveis; adicionar `currentDateStr` às chaves do `remember` e mover o cálculo para `viewModelScope` com `Dispatchers.Default` (ou `getLogsSince`).

### C9 🟡 Widget × relatório: regras de adesão diferentes
- No widget, dias anteriores à criação do hábito aparecem como **vermelhos (não feito)** (`!isToday -> widget_dot_missed`), enquanto o relatório semanal ignora esses dias (README: "dias anteriores à criação não penalizam"). Também carrega `getAllLogs()` inteiro a cada atualização e faz `find` por hábito × dia.
- **Solução:** reutilizar a mesma função "dia elegível" (`createdAt`, dia programado) em widget e relatório; consultar `getLogsSince(date-6)`; mostrar ponto neutro antes da criação.

### C10 🟡 `ConsequenceDialog`: sucesso/falha decidido por palavras-chave
- `MainActivity` define `isSuccess = !msg.contains("Pular") && !msg.contains("atrasa") && !msg.contains("prejudica") && !msg.contains("sedentarismo")` sobre **texto digitado pelo usuário**. Uma consequência positiva como "Não atrasa meu dia" é exibida como fracasso.
- **Solução:** `_consequence` deve carregar o status (`DONE`/`SKIPPED`) junto da mensagem, ex.: `data class Consequence(val habitName: String, val message: String, val isPositive: Boolean)`.

### C11 🟡 Alarme anti-procrastinação e `TextNormalizer`
- `AlarmDismissModal`: o botão fica `enabled = isMatch`, então o ramo `else { attempts++ }` é **código morto** — `attempts` é sempre 1 e a métrica de `AlarmEvent.dismissAttempts` não significa nada.
- Digitar a frase marca o hábito como **DONE** ("DESLIGAR ALARME E FAZER"), sem que o usuário tenha feito nada. Para o alarme de teste (`onTriggerTestAlarm`), isso grava DONE num hábito real.
- A frase está visível na tela (e no placeholder) e pode ser colada: o atrito é baixo.
- `TextNormalizer.normalize` remove tudo fora de `[a-z0-9\s]`; frases em alfabeto não latino, só com emoji/pontuação, viram `""` e `isPhraseMatch` retorna `true` para qualquer entrada (inclusive vazia).
- **Solução:** registrar o evento do alarme sem marcar DONE (ou perguntar "você fez?"); desabilitar colar (ou exigir digitação com `readOnly` + teclado customizado, se a intenção é atrito); incrementar `attempts` quando o texto digitado estiver errado ao tocar em "Enviar" (botão sempre habilitado); validar `dismissPhrase` ao salvar (mín. 10 caracteres alfanuméricos); usar `\p{L}\p{N}` na normalização; `isPhraseMatch` deve retornar `false` se o alvo normalizado for vazio.

### C12 🔵 Detalhes de domínio
- Tipos personalizados: a chave do tipo é o próprio nome digitado. Nomes como "Foco" colidem com tipos padrão em `getTypeLabel` (que usa `uppercase()`), e renomear um tipo órfã os hábitos. Separador `"||"` pode ser digitado pelo usuário. Usar IDs estáveis.
- `setAllHabitsShowInWidget(show)` sempre mostra "Todos os hábitos foram ativados no widget!", mesmo com `show = false`.
- `checkInHabit` não valida se o hábito está programado para hoje nem ativo.
- Semântica do sono: o registro é guardado na data de **acordar**, mas o lembrete obrigatório às 22:00 cobra "Registro de sono" do dia corrente e o texto diz "antes de dormir". Definir claramente a data de referência.
- Dados de exemplo gravam `durationMinutes = 480` com 23:30→07:00 (450 min reais).

---

## 5. Alarmes, notificações e segundo plano

### D1 🟠 Permissão de notificações nunca é solicitada
- `POST_NOTIFICATIONS` está no manifesto, mas não há `RequestPermission`/`requestPermissions` em nenhum arquivo. No Android 13+ (API 33), sem o pedido em tempo de execução, **nenhuma notificação aparece**; `AlarmReceiver` ainda captura `SecurityException` e segue em silêncio.
- **Solução:** pedir a permissão no onboarding/ao ativar lembretes (`rememberLauncherForActivityResult(RequestPermission())`), checar `NotificationManagerCompat.areNotificationsEnabled()` e mostrar aviso em ajustes com atalho para `ACTION_APP_NOTIFICATION_SETTINGS`.

### D2 🟠 Alarmes "exatos" nunca são exatos
- `AlarmScheduler` chama `canScheduleExactAlarms()` (API 31+), mas o manifesto não declara `SCHEDULE_EXACT_ALARM` nem `USE_EXACT_ALARM`. O retorno é sempre `false`, então cai em `setAndAllowWhileIdle` (inexato; em Doze pode atrasar vários minutos). O lembrete obrigatório usa inexato sempre. O "Adiar 15m" também.
- **Solução:** declarar `SCHEDULE_EXACT_ALARM`, direcionar o usuário para `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` quando `canScheduleExactAlarms()` for falso e registrar um receiver para `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` que chame o reagendamento. (`USE_EXACT_ALARM` é restrito pela política do Google Play a apps de despertador/agenda.) Se precisão de minutos não for crítica, documentar isso na UI.

### D3 🟠 Reagendamento após o boot não é aguardado
- `AlarmReceiver` (BOOT_COMPLETED) chama `AlarmScheduler.rescheduleAllAlarms(context)`, que internamente faz `CoroutineScope(IO).launch { ... }` e retorna na hora; o `finally` chama `pendingResult.finish()` antes do trabalho terminar. O sistema pode encerrar o processo e **os alarmes não são recriados**.
- **Solução:** transformar `rescheduleAllAlarms` em `suspend fun` (sem scope próprio) e chamá-la dentro da coroutine do receiver, antes de `finish()`. Reagendar também em `ACTION_TIMEZONE_CHANGED` e `ACTION_TIME_CHANGED`.

### D4 🟠 A cadeia de lembretes se rompe e o "Adiar" sobrescreve o próximo alarme
- Em `handleHabitReminder`, o reagendamento do próximo disparo só acontece **depois** de várias saídas antecipadas (`return` quando notificações/lembretes estão desativados, hábito inativo/removido, ou `!habit.isScheduledOn(hoje)`). Cada `return` mata a cadeia; só `rescheduleAllAlarms` (abrir o app) a restaura.
- `handleSnooze` cria um `PendingIntent` com **mesmo requestCode e mesma action** do lembrete regular (`habitId.toInt()` + `ACTION_HABIT_REMINDER`): o alarme de +15 min substitui o do próximo dia. Se o adiamento cruzar a meia-noite para um dia não programado, `isScheduledOn` falha e **o hábito para de lembrar**.
- `scheduleHabitReminder` retorna sem cancelar quando `calculateNextTriggerTime` é `null` (ex.: dias vazios): permanece um alarme antigo.
- **Solução:** reagendar o próximo disparo como **primeira ação** do `handleHabitReminder`, antes de qualquer filtro; usar requestCode/action distintos para soneca (`ACTION_HABIT_SNOOZE_FIRE`, `habitId * 10 + 3`); cancelar o alarme quando não houver próximo horário.

### D5 🟡 O "alarme anti-procrastinação" só existe com o app aberto
- `AlarmDismissModal`/`OverdueAlarmDialog` são diálogos dentro da `MainActivity`, ativados por um tick de 15 s. Com o app fechado, só chega uma notificação comum (sem `fullScreenIntent`, sem som de alarme, sem foreground service). O som usa `ToneGenerator` no stream de alarme.
- **Solução:** se o objetivo é acordar/forçar atenção, usar notificação com `setFullScreenIntent` + canal de alta importância e som de alarme (`USAGE_ALARM`), ou deixar claro na UI que são "lembretes" e não alarmes.

### D6 🔵 Detalhes das notificações
- `setSmallIcon(R.drawable.ic_launcher_foreground)`: a camada de foreground do ícone adaptativo aparece como quadrado branco/vazio na barra de status. Criar um ícone monocromático dedicado.
- A notificação carrega `from_notification` e `habit_id`, mas `MainActivity` só trata `from_widget`: tocar na notificação não leva ao hábito.
- Na hora de salvar um hábito, o app agenda lembrete mesmo com `notificationsEnabled = false`.
- Ao concluir o hábito no app, a notificação continua na bandeja.

---

## 6. Interface, ciclo de vida e desempenho

### E1 🟠 `isFromWidget` fica "preso" e silencia todos os alertas
- **Onde:** `MainActivity.setContent { RotinaUpApp(intent = intent) }` + `val isFromWidget = remember(intent) { intent?.getBooleanExtra("from_widget", false) == true }`.
- **Problema:** `intent` é lido uma vez na composição e não é estado; `onNewIntent` chama `setIntent`, mas **não dispara recomposição**. Se o app foi aberto pelo widget, `isFromWidget` permanece `true` enquanto a Activity viver (e após rotação, porque o extra continua no `Intent`). Resultado: `overdueHabitForAlarm`, `isMandatoryOverdueForAlarm` e o aviso de dados de exemplo ficam **desligados** até a Activity ser recriada.
- **Solução:** transformar em evento consumível:
  ```kotlin
  private var fromWidget by mutableStateOf(false)
  private fun handleWidgetIntent(i: Intent?) {
      if (i?.getBooleanExtra("from_widget", false) == true) {
          i.removeExtra("from_widget")          // consome
          viewModel.snoozeOverdue(5)            // o snooze de 5 min já cobre o objetivo
      }
  }
  ```
  e remover `isFromWidget` das condições (o snooze já evita o pop-up imediato).

### E2 🟡 Telas "cheias" removem o `NavHost` e perdem estado
- `RotinaUpApp` usa `return` antecipado para PIN, carregamento e `DeepFocusScreen`. Isso tira o `NavHost` da composição: ao voltar, a navegação reinicia em `"today"` e o estado local das telas (formulários de sono/diário, filtros) se perde.
- **Solução:** manter o `NavHost` sempre composto e sobrepor as telas cheias com `Box`/`AnimatedVisibility`, ou transformar foco e bloqueio em destinos do `NavHost`.

### E3 🟡 Timer do Modo Foco frágil
- Estado em `remember` (não `rememberSaveable`), sem `configChanges`: **girar a tela reinicia o cronômetro**. A contagem por `delay(1000)` acumula deriva e pausa se o app for para segundo plano/Doze.
- `maxOf(60, minutes*60)` transforma `durationMinutes = 0` (hábito "Não tirar cochilos") em 1 minuto, enquanto `null` vira 25 min.
- **Solução:** guardar `endAtElapsedRealtime` (calculado uma vez) em `rememberSaveable`/ViewModel e derivar o restante do relógio; manter a tela ligada com `FLAG_KEEP_SCREEN_ON`; tratar duração 0/`null` explicitamente.

### E4 🟡 Coleta de estado e ticks redundantes
- 41 usos de `collectAsState()` e nenhum `collectAsStateWithLifecycle()` (a dependência `lifecycle-runtime-compose` já está no projeto): coleta continua com o app em segundo plano.
- Três mecanismos de "tick": `ACTION_TIME_TICK` (receiver), `delay(15000)` em `LaunchedEffect` e `onResume`. Há ainda `delay(600)` como "tempo de assentamento" do Room — gambiarra; o `AuthState.Loading` já cobre isso se o estado inicial dos fluxos for tratado como "carregando".
- **Solução:** `collectAsStateWithLifecycle()`; um único ticker ligado ao ciclo de vida; trocar o `delay(600)` por flags de "primeira emissão" (ex.: `UiState.Loading`).

### E5 🟡 Barras do sistema não seguem o tema do app
- `enableEdgeToEdge()` usa o estilo padrão (segue o tema do sistema). Com tema do app ≠ tema do sistema (o app tem seletor próprio), os ícones da barra de status ficam ilegíveis (escuros sobre fundo escuro, ou o inverso). O tema XML `Theme.MyApplication` também causa flash de cor na abertura.
- **Solução:** `enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(...))` chamado em `LaunchedEffect(isDarkTheme)`, e `windowBackground` coerente no tema XML.

### E6 🔵 Acessibilidade e i18n
- 0 usos de `stringResource` (todo o texto em português está no código) e 73 `contentDescription = null` (alguns decorativos, outros em botões). Cores do widget fixas em código (sem dark/light).
- **Solução:** mover textos para `strings.xml` (já prepara EN/ES), revisar descrições em ícones interativos, usar `Modifier.semantics` nos pontos de status do widget.

---

## 7. Testes

### F1 🟡 Testes que não protegem o que importa
- `ExampleUnitTest` **reimplementa** as fórmulas (duração do sono, desvio-padrão do horário de deitar) dentro do próprio teste, sem chamar o código de produção: passam mesmo que `MainViewModel` seja quebrado.
- Não há testes de migração (só existe `schemas/.../7.json`; faltam os JSON das versões 1–6 e `MigrationTestHelper`), nem de `AlarmScheduler`, `AlarmReceiver`, lockout da tela de PIN, widget ou do seed.
- `WeeklyReportTest` instancia `MainViewModel(app)`, o que dispara no `init` o seed, `rescheduleAllAlarms` e o banco real de arquivo (`rotinaup.db`) sob Robolectric: teste **não hermético**, dependente da data atual.
- **Solução:** extrair regras puras (`WeeklyReportCalculator`, `SleepDurationCalculator`, `NextTriggerCalculator`) em classes sem Android, receber `Clock` por parâmetro e testar com datas fixas; injetar DAO/DB em memória no ViewModel; adicionar `MigrationTestHelper` com os schemas exportados; cobrir: meia-noite/fuso, soneca cruzando a meia-noite, hábito criado no meio da semana, PIN com tentativas, e a primeira execução (seed + flags).

---

## 8. Plano de correção sugerido

**Fase 0 — Desbloquear o projeto (1–2 dias)**
1. Commitar `SettingsScreen.kt`, `gradlew*`, wrapper jar; corrigir `debug.keystore` (A1–A3).
2. Revisar `.gitignore` e remover Secrets/Gemini se não usados (A6).

**Fase 1 — Segurança e integridade (3–5 dias)**
3. Corrigir `onDigit` e levar o lockout para o ViewModel (B1, B4).
4. Re-lock por tempo em segundo plano + `FLAG_SECURE` (B2).
5. Corrigir seed/flags e `clearDummyContent` com coluna `isSample` (C1, C2, C7).
6. Validar horários e durações com `TimePicker` + migração de dados existentes (C3).

**Fase 2 — Lembretes confiáveis (3–5 dias)**
7. Pedir `POST_NOTIFICATIONS`; fluxo de alarme exato (D1, D2).
8. `rescheduleAllAlarms` como `suspend`, reagendar primeiro, soneca com PendingIntent próprio, receivers de fuso/horário (D3, D4).
9. Corrigir `isFromWidget` (E1).

**Fase 3 — Consistência e manutenção (1–2 semanas)**
10. FKs + migração 7→8; regras únicas de "dia elegível" para widget e relatório (C4, C8, C9).
11. Criptografia do banco/biometria, backup com WAL (B3, C5).
12. Extrair lógica do `MainViewModel` (hoje ~960 linhas), atualizar dependências, `collectAsStateWithLifecycle`, strings em recursos (A7, E4, E6).
13. Reescrever os testes (F1) e completar o CI (A4).

### Checklist mínimo de regressão
- [ ] Instalação limpa: aparece o aviso de dados de exemplo; "Limpar" remove só os exemplos.
- [ ] 3 PINs errados seguidos (com 4 dígitos) acionam o bloqueio; mudar a hora do aparelho não o remove.
- [ ] App em segundo plano 1 min → pede PIN ao voltar.
- [ ] Reiniciar o aparelho → lembretes continuam chegando (Android 13/14, com e sem Doze).
- [ ] Abrir pelo widget uma vez → alertas de atraso continuam funcionando depois.
- [ ] Horário inválido ("8:7", "25:61") é recusado ao salvar hábito/sono.
- [ ] Adiar lembrete às 23:50 em hábito de dias úteis → próximo dia útil continua agendado.
- [ ] Excluir hábito → some da bandeja, widget e relatório; sem logs órfãos.
- [ ] Girar a tela no Modo Foco → cronômetro continua.
- [ ] Backup e restauração em aparelho limpo preservam os últimos registros.

---

## 9. Limitações desta análise

- Análise estática: não houve compilação nem execução; o comportamento em dispositivos reais (Doze, fabricantes que restringem alarmes) deve ser confirmado.
- Arquivos que não aparecem no material recebido (`SettingsScreen.kt`, `gradlew`, `.gitignore`, `debug.keystore`) podem existir apenas localmente; os itens afetados estão marcados.
- As telas foram revisadas com foco em lógica, estado e ciclo de vida; não foi feita revisão visual/UX.
