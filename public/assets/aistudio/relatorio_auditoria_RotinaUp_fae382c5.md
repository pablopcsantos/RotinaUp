# Relatório de Auditoria Técnica — RotinaUp

**Data da análise:** 20/10/2026  
**Repositório:** https://github.com/pablopcsantos/RotinaUp  
**Branch analisada:** `main`  
**Tipo de aplicação:** Android nativo, Kotlin, Jetpack Compose, Room e Gradle

---

## 1. Resumo executivo

O RotinaUp possui uma base funcional relevante: telas organizadas por áreas do produto, persistência local com Room, `ViewModel`, `StateFlow`, componentes Compose e um widget Android. Entretanto, a revisão estática encontrou riscos importantes de integridade dos dados, segurança, consistência temporal, confiabilidade das métricas e qualidade da validação.

### Prioridades recomendadas

| Prioridade | Achado | Impacto potencial |
|---|---|---|
| **P0 — Crítica** | PIN armazenado como texto simples | A proteção local pode ser contornada por inspeção dos dados ou do processo; o PIN não deve ser tratado como autenticação forte. |
| **P1 — Alta** | `fallbackToDestructiveMigration()` junto às migrações Room | Uma versão de banco não coberta pelas migrações pode causar perda de dados locais. |
| **P1 — Alta** | Relatório semanal usa denominadores e dados substitutos que podem distorcer resultados | Indicadores de adesão e sono podem comunicar conclusões incorretas. |
| **P1 — Alta** | Lembretes são avaliados principalmente enquanto a interface está ativa | Não há evidência, no fluxo analisado, de agendamento confiável em segundo plano; alertas podem não ocorrer com o app fechado. |
| **P1 — Alta** | Testes automatizados são insuficientes para a lógica do produto | Regressões em banco, datas, cálculos e fluxos de interface podem passar despercebidas. |
| **P2 — Média** | Datas de “hoje” são calculadas uma vez na criação do `MainViewModel` | Uma instância que atravesse a meia-noite pode continuar consultando o dia anterior. |
| **P2 — Média** | Widget executa consultas e atualizações assíncronas com tratamento de erro silencioso | O widget pode exibir dados desatualizados ou falhar sem diagnóstico útil. |
| **P2 — Média** | Classes extensas concentram responsabilidades | Aumenta o custo de manutenção e a chance de regressões. |
| **P2 — Média** | Configuração de build e assinatura de release depende de ambiente externo não documentado | Builds de release podem falhar quando faltam keystore ou variáveis de ambiente. |
| **P2 — Média** | Documentação de entrada e instruções de execução parecem ausentes na raiz observada | Dificulta reproduzir builds e entender a configuração do projeto. |

**Importante:** esta é uma revisão estática do conteúdo consultado no GitHub. Não foi executado um build, nem foram realizados testes em dispositivo, testes de penetração ou análise dinâmica. Os pontos dependentes de execução estão identificados como riscos a validar.

---

## 2. Escopo e método

Foram consultados a árvore do repositório e arquivos representativos, incluindo:

- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/ui/viewmodel/HabitoViewModel.kt`
- `app/src/main/java/com/example/data/db/AppDatabase.kt`
- `app/src/main/java/com/example/data/db/HabitoDao.kt`
- `app/src/main/java/com/example/data/model/UserSettings.kt`
- `app/src/main/java/com/example/widget/RotinaUpWidgetProvider.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/test/java/com/example/ExampleUnitTest.kt`

Referências diretas:
- [MainActivity.kt](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/MainActivity.kt)
- [MainViewModel.kt](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt)
- [AppDatabase.kt](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/db/AppDatabase.kt)
- [HabitoDao.kt](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/db/HabitoDao.kt)
- [UserSettings.kt](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/model/UserSettings.kt)
- [RotinaUpWidgetProvider.kt](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/widget/RotinaUpWidgetProvider.kt)
- [AndroidManifest.xml](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/AndroidManifest.xml)
- [app/build.gradle.kts](https://github.com/pablopcsantos/RotinaUp/blob/main/app/build.gradle.kts)
- [libs.versions.toml](https://github.com/pablopcsantos/RotinaUp/blob/main/gradle/libs.versions.toml)
- [Teste unitário atual](https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/test/java/com/example/ExampleUnitTest.kt)

---

## 3. Achados detalhados

### 3.1. [P1] Migração do Room pode apagar dados existentes

**Arquivo:** `app/src/main/java/com/example/data/db/AppDatabase.kt`

**Evidência:** o banco está na versão `5`, declara migrações `2→3`, `3→4` e `4→5`, mas também chama `.fallbackToDestructiveMigration()`.

**Problema:** se uma instalação possuir uma versão de banco sem caminho de migração compatível, o Room poderá recriar o banco e eliminar os dados locais. Para um aplicativo que registra hábitos, sono e avaliações diárias, isso representa perda de histórico do usuário.

**Solução recomendada:**
1. Remover o fallback destrutivo do fluxo de produção, salvo se a perda de dados for uma decisão explícita e comunicada.
2. Criar uma migração para cada alteração futura de esquema.
3. Ativar a exportação de esquemas Room (`exportSchema = true`) e versionar os arquivos gerados.
4. Adicionar testes de migração com bancos de versões anteriores.
5. Definir uma estratégia de backup/exportação e testar restauração.

**Critério de aceite:** atualizar uma instalação de cada versão suportada preserva hábitos, check-ins, registros de sono, diário e configurações.

### 3.2. [P1] PIN armazenado em texto simples e proteção limitada

**Arquivos:** `UserSettings.kt`, `MainActivity.kt` e `AuthPinLockScreen.kt`

**Evidência:** `UserSettings` possui o campo `pinCode: String?`; a tela principal compara o PIN configurado e o fornece à tela de bloqueio. O valor não é representado como hash ou derivado criptográfico no modelo consultado.

**Problema:** um PIN guardado em texto simples não oferece proteção robusta contra alguém com acesso aos dados do aplicativo ou a um dispositivo comprometido. Além disso, um bloqueio implementado apenas na interface não equivale a uma fronteira de segurança para dados sensíveis.

**Solução recomendada:**
1. Evitar persistir o PIN em texto simples.
2. Preferir Android Keystore para proteger material criptográfico e, se a finalidade for desbloqueio local, usar derivação segura de chave com salt e parâmetros adequados, em vez de comparação direta do PIN.
3. Considerar autenticação biométrica do sistema por `BiometricPrompt` quando apropriado.
4. Aplicar limitação progressiva de tentativas e atraso após falhas; evitar bloqueios permanentes que facilitem negação de serviço.
5. Definir claramente o que o PIN protege. Não afirmar que ele protege banco, arquivos ou backups se esses recursos não estiverem protegidos independentemente.
6. Verificar se backups Android incluem dados que deveriam permanecer protegidos.

**Critério de aceite:** o PIN não aparece em texto simples nos dados persistidos e a política de desbloqueio é testada após reinício, recriação da atividade e falhas repetidas.

### 3.3. [P1] Cálculo do relatório semanal pode produzir indicadores enganosos

**Arquivo:** `MainViewModel.kt`, função `computeWeeklyReport`

**Evidências observadas:**
- `scheduledDays` é forçado a pelo menos `1` por meio de `.coerceAtLeast(1)`, mesmo quando o hábito não está programado para nenhum dia da semana analisada.
- O cálculo de adesão soma `DONE` e metade de `PARTIAL`, mas usa como denominador os dias programados, não a quantidade de ocorrências efetivamente elegíveis em todas as situações.
- Quando não existem registros de sono, a média recebe um valor fixo (`7.4`) em vez de representar ausência de dados.
- A variável `bedtimeVar` recebe valores fixos conforme a quantidade de registros, não uma variabilidade calculada a partir dos horários observados.

**Problema:** um painel pode mostrar um percentual ou uma média aparentemente real sem que haja amostra suficiente. Hábito sem dias válidos, semana incompleta e ausência de sono precisam de tratamento explícito.

**Solução recomendada:**
1. Definir matematicamente adesão: numerador, denominador, dias programados e estados que contam como ocorrência válida.
2. Não substituir ausência de dados por médias inventadas. Retornar estado “sem dados” ou `null`.
3. Calcular a variabilidade dos horários a partir dos registros reais, levando em conta a passagem pela meia-noite.
4. Distinguir “não realizado”, “não programado”, “sem registro” e “não aplicável”.
5. Criar testes unitários para semanas vazias, hábitos sem dias configurados, hábito criado no meio da semana, dias não programados, registros parciais e sono ausente.

**Critério de aceite:** cada métrica pode ser explicada por uma fórmula testada e nenhum dado inexistente é apresentado como observado.

### 3.4. [P1 — validar em execução] Lembretes dependem da atividade da interface

**Arquivo:** `MainActivity.kt`

**Evidência:** a verificação de atraso utiliza estados Compose e um `LaunchedEffect` que atualiza o relógio a cada 15 segundos. Os diálogos são apresentados pela interface enquanto a composição está ativa.

**Problema:** esse mecanismo, por si só, não garante que o lembrete será entregue com o aplicativo em segundo plano, processo encerrado ou aparelho reiniciado. O manifesto declara `POST_NOTIFICATIONS`, mas a presença da permissão não comprova que notificações estejam efetivamente agendadas e entregues.

**Solução recomendada:**
1. Confirmar o comportamento esperado: lembrete visual apenas durante o uso ou alarme/notificação em segundo plano.
2. Se for necessário funcionar em segundo plano, avaliar `WorkManager` para trabalho adiável e `AlarmManager` para horários específicos, respeitando as restrições atuais do Android.
3. Criar canal de notificação, solicitar permissão em tempo de execução quando necessário e tratar a recusa.
4. Reagendar alarmes após reinicialização e mudanças relevantes de horário, caso o desenho escolhido exija isso.
5. Separar a regra de negócio que identifica atrasos do mecanismo que entrega o lembrete.
6. Testar app aberto, em segundo plano, processo encerrado, aparelho reiniciado, permissão negada e modo de economia de bateria.

**Critério de aceite:** a entrega e as limitações dos lembretes são documentadas e verificadas nos cenários de ciclo de vida definidos pelo produto.

### 3.5. [P2] A data corrente fica congelada na instância do `MainViewModel`

**Arquivo:** `MainViewModel.kt`

**Evidência:** `todayDateStr` é inicializada uma única vez com `DateUtils.getTodayDateString(0)` e reutilizada nos fluxos de registros do dia.

**Problema:** se o processo ou a instância do `ViewModel` continuar ativo durante a meia-noite, os fluxos que dependem dessa data podem continuar observando o dia anterior. Atualizar somente `currentTimeStr` não atualiza `todayDateStr`.

**Solução recomendada:**
1. Centralizar a data operacional em um fluxo observável que detecte a mudança de dia.
2. Atualizar consultas de hoje quando a data mudar e quando o aplicativo retornar ao primeiro plano.
3. Evitar calcular a data em propriedades imutáveis de longa duração quando o comportamento exige mudança diária.
4. Cobrir mudança de dia, mudança de fuso horário e horário de verão quando aplicável com testes usando relógio injetável.

**Critério de aceite:** após atravessar a meia-noite, os registros e lembretes usam a data correta sem reiniciar o aplicativo.

### 3.6. [P2] Widget pode exibir dados desatualizados e oculta falhas

**Arquivo:** `RotinaUpWidgetProvider.kt`

**Evidências observadas:**
- O widget inicia uma nova `CoroutineScope(Dispatchers.IO)` para atualizar os dados.
- Exceções são capturadas com `catch (_: Throwable)` e não geram diagnóstico.
- O texto da data é preenchido com `DateUtils.getNowTimeStr()`, que aparenta ser uma função de horário, apesar de o campo se chamar `widget_date`.
- A atualização assíncrona pode terminar depois da atualização inicial, e não há garantia, no trecho analisado, de atualização periódica para refletir a passagem do tempo.

**Problema:** erros de consulta ou de atualização ficam invisíveis e o usuário pode ver informação antiga ou uma data que na verdade representa horário.

**Solução recomendada:**
1. Usar logging estruturado, sem registrar dados pessoais ou notas do usuário.
2. Revisar o campo de data e utilizar um formatador de data apropriado.
3. Definir gatilhos explícitos para atualizar o widget após gravações, virada do dia e eventos suportados pelo Android.
4. Evitar escopos de coroutine sem ciclo de vida claro; avaliar uma estratégia compatível com o ciclo de vida de `AppWidgetProvider`.
5. Testar widget sem hábitos, com muitos hábitos, banco indisponível e processo recriado.

**Critério de aceite:** erros são diagnosticáveis e a informação exibida corresponde ao estado atual do banco.

### 3.7. [P2] Exclusão de hábito e histórico precisa de política explícita

**Arquivos:** `HabitoDao.kt`, `HabitoRepository.kt` e modelos de dados

**Evidência:** o DAO declara `deleteHabit(habit: Habito)`, enquanto `HabitLog` mantém `habitId`. No trecho consultado não foi demonstrada uma regra de chave estrangeira com exclusão em cascata ou uma política explícita de retenção do histórico.

**Problema:** é necessário verificar se a exclusão deixa registros históricos órfãos e como relatórios tratam logs sem o hábito correspondente. Apagar o histórico automaticamente também pode ser indesejável.

**Solução recomendada:**
1. Definir a semântica de exclusão: excluir também o histórico, arquivar o hábito ou preservar registros históricos.
2. Se o histórico for preservado, guardar os dados históricos necessários para renderizar relatórios mesmo depois da exclusão do hábito.
3. Se houver cascata, declarar e testar as chaves estrangeiras e a transação correspondente.
4. Adicionar testes para excluir hábito com e sem logs e verificar relatórios anteriores.

**Critério de aceite:** a exclusão segue uma regra documentada e não causa resultados inconsistentes nos relatórios.

### 3.8. [P2] Duplicação de lógica entre ViewModels

**Arquivos:** `MainViewModel.kt` e `HabitoViewModel.kt`

**Evidência:** os dois ViewModels expõem operações semelhantes de criação, edição, exclusão, ativação e check-in de hábitos, além de fluxos derivados do repositório.

**Problema:** manter regras duplicadas aumenta o risco de que uma tela aplique validações, notificações ao widget ou efeitos colaterais diferentes de outra.

**Solução recomendada:**
1. Identificar se `HabitoViewModel` ainda é utilizado e remover código morto se não houver uso.
2. Concentrar regras e efeitos de negócio em casos de uso ou em uma camada de repositório com contratos claros.
3. Manter os ViewModels responsáveis por coordenar estado de interface, não por duplicar regras.
4. Definir testes compartilhados para os mesmos casos de uso.

**Critério de aceite:** cada operação de negócio possui uma implementação principal e comportamento consistente em todas as telas.

### 3.9. [P2] Arquivos extensos concentram responsabilidades

**Arquivos:** `MainActivity.kt`, `MainViewModel.kt`, `SettingsScreen.kt`, `SleepScreen.kt`, `TodayScreen.kt` e `WeeklyReportScreen.kt`

**Evidência:** alguns arquivos consultados têm dezenas de milhares de caracteres e misturam vários estados, componentes, validações e regras de negócio.

**Problema:** arquivos grandes não são, isoladamente, um defeito funcional; porém, quando acumulam responsabilidades, dificultam revisão, testes isolados e localização de regressões.

**Solução recomendada:**
1. Extrair componentes Compose reutilizáveis e componentes de tela menores.
2. Mover cálculos puros para funções ou classes de domínio independentes da UI.
3. Separar formatação, validação, persistência e regras de lembrete.
4. Adotar convenções de nomenclatura consistentes — por exemplo, revisar a coexistência de `Habit` e `Habito` se ambas representarem a mesma entidade.
5. Refatorar em mudanças pequenas, preservando comportamento por meio de testes.

**Critério de aceite:** cada módulo tem responsabilidade definida e as regras centrais podem ser testadas sem inicializar toda a interface.

### 3.10. [P1] Cobertura de testes não representa a complexidade do aplicativo

**Arquivo:** `app/src/test/java/com/example/ExampleUnitTest.kt`

**Evidência:** o teste unitário de exemplo verifica somente `2 + 2 == 4`. Há também um teste Robolectric de exemplo, mas isso não demonstra cobertura das regras reais do produto.

**Problema:** cálculos de adesão, cruzamento de meia-noite, migrações, PIN, check-ins, alarmes e atualização de widget são comportamentos com risco de regressão e não estão demonstrados por testes específicos no conjunto consultado.

**Solução recomendada:**
1. Substituir ou complementar testes de exemplo por testes unitários de regras de domínio.
2. Testar cálculos de relatório com dados conhecidos e resultados esperados.
3. Testar DAOs e migrações com banco em memória ou fixtures de esquemas anteriores.
4. Testar fluxos de interface críticos com Compose UI Test.
5. Criar testes de integração para check-in, atualização do widget e persistência após recriação.
6. Configurar CI para executar testes e build em cada pull request.

**Critério de aceite:** os principais comportamentos possuem testes reproduzíveis, e a pipeline impede merge quando os testes ou o build falham.

### 3.11. [P2] Build de release depende de configuração externa pouco documentada

**Arquivo:** `app/build.gradle.kts`

**Evidência:** a configuração `release` usa `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`, com caminho padrão `${rootDir}/my-upload-key.jks`; o alias está fixado em `upload`. O arquivo `.gitignore` ignora `debug.keystore`, mas isso não comprova a existência do keystore de release nem a configuração das variáveis.

**Problema:** a compilação de release pode falhar se o arquivo ou as credenciais não estiverem presentes. Um processo de build reproduzível precisa tornar os pré-requisitos claros sem incluir segredos no repositório.

**Solução recomendada:**
1. Documentar os pré-requisitos para `assembleDebug` e `assembleRelease`.
2. Validar as variáveis de ambiente e emitir erros claros quando ausentes.
3. Manter keystores e senhas fora do Git; usar secrets do ambiente de CI.
4. Não usar uma chave de debug para assinar versões distribuídas.
5. Testar build de debug e release em ambiente limpo.
6. Avaliar habilitar minificação e otimização apenas depois de configurar regras e testar o artefato final.

**Critério de aceite:** build debug reproduzível e release gerável em ambiente documentado sem segredos versionados.

### 3.12. [P2] Configuração de permissões e privacidade precisa ser validada

**Arquivo:** `AndroidManifest.xml`

**Evidência:** o manifesto declara `POST_NOTIFICATIONS`, `VIBRATE` e `WAKE_LOCK`, e define `android:allowBackup="true"` com regras de backup. A presença dessas configurações não comprova, isoladamente, uma falha.

**Riscos a validar:**
- Solicitação de permissão de notificações em tempo de execução em versões compatíveis.
- Conteúdo incluído em backup, especialmente configurações e PIN.
- Necessidade real de cada permissão declarada.
- Exposição de informações pessoais em notificações, logs ou widget.

**Solução recomendada:**
1. Revisar `backup_rules.xml` e `data_extraction_rules.xml` junto aos dados efetivamente persistidos.
2. Excluir dados sensíveis do backup ou protegê-los de forma apropriada.
3. Remover permissões não utilizadas e documentar as necessárias.
4. Criar uma política simples de privacidade que descreva armazenamento local, backup e exclusão de dados.

**Critério de aceite:** cada permissão tem finalidade identificada e os dados sensíveis seguem uma política de retenção e backup testada.

### 3.13. [P2] Falta de documentação operacional na raiz observada

**Evidência:** a listagem da raiz consultada apresenta `.env.example`, `.gitignore`, `app/`, arquivos Gradle e `metadata.json`; não foi identificado `README.md` na raiz dessa listagem.

**Problema:** sem instruções claras, colaboradores podem não saber qual JDK/Android SDK usar, como executar os testes, como configurar secrets ou como gerar um APK.

**Solução recomendada:**
1. Criar `README.md` com descrição, funcionalidades, arquitetura e requisitos.
2. Documentar comandos para sincronizar, compilar e testar o projeto.
3. Explicar configuração de assinatura sem publicar segredos.
4. Incluir limitações conhecidas, permissões e comportamento dos lembretes.
5. Registrar um fluxo de contribuição e uma lista de verificações antes de release.

**Critério de aceite:** uma pessoa com ambiente Android configurado consegue compilar e testar o projeto seguindo apenas a documentação.

---

## 4. Plano de correção sugerido

### Fase 1 — Preservar dados e reduzir riscos de segurança

- [ ] Revisar e remover o fallback destrutivo de migração em produção.
- [ ] Habilitar exportação de esquema e adicionar testes de migração.
- [ ] Substituir o armazenamento simples do PIN por uma abordagem segura.
- [ ] Auditar backup e exclusão de dados sensíveis.
- [ ] Documentar e validar o processo de assinatura de release.

### Fase 2 — Corrigir a confiabilidade dos dados

- [ ] Formalizar as fórmulas do relatório semanal.
- [ ] Remover médias e variabilidades artificiais.
- [ ] Corrigir a atualização da data após a meia-noite.
- [ ] Definir política de exclusão e retenção do histórico de hábitos.
- [ ] Adicionar testes para os casos-limite dos cálculos.

### Fase 3 — Tornar lembretes e widget previsíveis

- [ ] Definir os requisitos de entrega dos lembretes em segundo plano.
- [ ] Implementar e testar o mecanismo Android apropriado, se necessário.
- [ ] Revisar permissões e canais de notificação.
- [ ] Melhorar logging de falhas no widget sem expor dados pessoais.
- [ ] Validar atualização após check-ins, mudança de dia e reinicialização.

### Fase 4 — Manutenção e prevenção de regressões

- [ ] Reduzir duplicação entre ViewModels.
- [ ] Dividir arquivos extensos por responsabilidade.
- [ ] Adicionar testes unitários, de banco e de interface.
- [ ] Configurar integração contínua para build e testes.
- [ ] Criar README e checklist de release.

---

## 5. Matriz mínima de testes de regressão

| Área | Cenário | Resultado esperado |
|---|---|---|
| Banco | Atualizar da versão 2 até a versão atual | Dados preservados e esquema válido |
| Banco | Atualizar de cada versão suportada | Migração executada sem fallback destrutivo |
| PIN | Fechar e reabrir o aplicativo | Bloqueio segue a política definida |
| PIN | Errar o PIN repetidamente | Limitação de tentativas funciona |
| Datas | Aplicativo permanece aberto durante a meia-noite | Consultas passam a usar o novo dia |
| Hábitos | Hábito não programado para o dia atual | Não entra indevidamente no denominador |
| Relatório | Nenhum registro de sono na semana | Interface mostra ausência de dados, não média fictícia |
| Relatório | Hábitos com estados `DONE`, `PARTIAL` e `SKIPPED` | Percentuais seguem a fórmula documentada |
| Lembretes | Aplicativo em segundo plano | Comportamento corresponde ao requisito declarado |
| Lembretes | Permissão de notificação negada | Aplicativo informa a limitação sem falhar |
| Widget | Nenhum hábito selecionado | Estado vazio correto e sem crash |
| Widget | Erro de leitura do banco | Falha diagnosticável e interface segura |
| Exclusão | Excluir hábito com histórico | Política de retenção é respeitada |
| Release | Build em ambiente limpo | Pré-requisitos ausentes são detectados claramente |

---

## 6. Sequência de execução técnica

Sugestão de sequência para a equipe ou para uma IA de desenvolvimento:

1. **Estabelecer uma linha de base:** executar build e testes atuais e registrar erros reproduzíveis.
2. **Proteger os dados existentes:** tratar migrações Room e definir a política de backup antes de alterar entidades.
3. **Corrigir a semântica dos dados:** revisar datas, fórmulas e estados de ausência de registro.
4. **Reforçar a segurança local:** redesenhar o armazenamento e o fluxo do PIN.
5. **Validar o ciclo de vida Android:** implementar e testar a política de lembretes e as atualizações do widget.
6. **Refatorar com testes:** reduzir duplicação e tamanho das classes sem misturar refatoração com mudanças de comportamento.
7. **Automatizar a verificação:** adicionar CI e checklist de release.

Não é recomendável começar por uma grande reescrita da arquitetura. Corrigir primeiro os riscos de perda de dados e de métricas incorretas tende a gerar mais segurança com menor risco de regressão.

---

## 7. Limitações e validações pendentes

Este documento não afirma que todos os riscos descritos já foram reproduzidos em execução. Em particular, ainda é necessário:

- executar `./gradlew test` e `./gradlew assembleDebug`;
- testar `assembleRelease` com o ambiente de assinatura documentado;
- analisar os arquivos completos de configuração de backup;
- verificar o comportamento real dos lembretes em segundo plano;
- inspecionar o restante das telas, DAOs e modelos em busca de falhas adicionais;
- testar migrações com bancos reais de versões anteriores;
- executar análise estática e dinâmica de segurança.

A árvore e os arquivos foram consultados na branch `main` do repositório público. Os resultados podem mudar com novos commits.

---

## 8. Conclusão

O RotinaUp já contém uma estrutura de aplicativo Android com persistência local e funcionalidades relevantes, mas precisa reforçar a proteção dos dados, a precisão das métricas e a confiabilidade de recursos dependentes do ciclo de vida do Android.

**As correções mais importantes são:**
1. eliminar o risco de perda silenciosa de dados em migrações;
2. não armazenar PIN em texto simples;
3. corrigir métricas que usam valores substitutos ou denominadores inadequados;
4. garantir que datas e lembretes respeitem a passagem do tempo e o ciclo de vida;
5. ampliar os testes e documentar o processo de build e release.

A conclusão definitiva sobre estabilidade só deve ser emitida após executar o projeto e reproduzir os cenários listados neste relatório.
