# Relatório de Auditoria Técnica e Melhorias — RotinaUp

**Data da análise:** 20 de outubro de 2026[span_0](start_span)[span_0](end_span)
**Repositório:** RotinaUp (Assistente Pessoal de Rotina & Hábitos)[span_1](start_span)[span_1](end_span)
**Tecnologias:** Kotlin, Jetpack Compose, Room Database, MVVM[span_2](start_span)[span_2](end_span)

Com base na análise estática do código-fonte e dos relatórios de auditoria previamente integrados ao projeto, foram identificadas vulnerabilidades e falhas arquiteturais. Abaixo detalhamos os problemas encontrados e as respectivas soluções propostas.

---

## 1. Segurança e Autenticação

### 1.1. Bypass de Autenticação na Inicialização
*   **Problema:** O estado de desbloqueio do aplicativo pode ser interpretado como "desbloqueado" provisoriamente durante a inicialização, pois as configurações são carregadas de forma assíncrona a partir do banco de dados Room[span_3](start_span)[span_3](end_span).
*   **Solução:** Implementar um estado explícito de inicialização (como `Loading`, `Locked`, e `Unlocked`)[span_4](start_span)[span_4](end_span). É imprescindível bloquear o acesso a qualquer tela protegida até que a configuração de autenticação real seja totalmente carregada do armazenamento local[span_5](start_span)[span_5](end_span).

### 1.2. Reset do Bloqueio de Força Bruta (Em Memória)
*   **Problema:** O contador de tentativas incorretas do PIN e o tempo de bloqueio (penalidade) podem estar utilizando estados voláteis em memória (como `remember` no Compose) em partes da UI, o que permite que o usuário zere as tentativas recriando a Activity ou reiniciando o processo[span_6](start_span)[span_6](end_span).
*   **Solução:** O estado de tentativas falhas e o _timestamp_ de expiração do bloqueio devem ser persistidos fora do ciclo de vida da interface, utilizando mecanismos como `SharedPreferences` ou `DataStore` (conforme implementado na classe `SecurityUtils`, mas que precisa ser garantido no fluxo da UI)[span_7](start_span)[span_7](end_span).

### 1.3. Armazenamento e Migração de PIN Legado
*   **Problema:** O modelo `UserSettings` ainda possui um campo `pinCode` em texto simples para retrocompatibilidade e a função de hash SHA-256 é considerada rápida demais para senhas curtas (baixa entropia)[span_8](start_span)[span_8](end_span). 
*   **Solução:** O sistema deve migrar definitivamente todas as credenciais para o padrão moderno implementado (`PBKDF2WithHmacSHA256` com _salt_ aleatório)[span_9](start_span)[span_9](end_span). O campo de texto simples deve ser preenchido como `null` assim que a credencial legada for validada com sucesso, removendo o PIN puro do armazenamento persistente[span_10](start_span)[span_10](end_span).

---

## 2. Regras de Negócio e Dados

### 2.1. Inconsistência no Relatório Semanal
*   **Problema:** A função `computeWeeklyReport` pode gerar distorções nas métricas de adesão[span_11](start_span)[span_11](end_span). Os erros incluem contabilizar dias anteriores à criação do hábito no denominador e somar registros concluídos (`DONE`) em dias em que o hábito não estava sequer programado[span_12](start_span)[span_12](end_span). Além disso, a ausência de dados de sono pode ser tratada de forma equivocada, distorcendo a geração de "medalhas[span_13](start_span)"[span_13](end_span).
*   **Solução:** Centralizar e alinhar a regra de dias elegíveis[span_14](start_span)[span_14](end_span). O numerador e o denominador das taxas de adesão devem processar estritamente o mesmo conjunto de datas (após a criação do hábito e apenas nos dias marcados)[span_15](start_span)[span_15](end_span). Para os relatórios de sono, dados inexistentes devem ser tratados como `null` (ou "sem dados"), e não como zero ou valores fictícios[span_16](start_span)[span_16](end_span).

### 2.2. Reinserção Acidental de Dados Demonstrativos
*   **Problema:** O método `seedInitialData()` pode ser acionado se o usuário apagar todos os hábitos manualmente, fazendo com que o app confunda um "banco vazio" com uma "primeira execução" e recrie todo o conteúdo de teste[span_17](start_span)[span_17](end_span).
*   **Solução:** Utilizar uma flag persistente, como `hasSeededInitialData`, para diferenciar a primeira instalação de uma exclusão intencional de todos os hábitos por parte do usuário[span_18](start_span)[span_18](end_span). Essa flag só deve permitir a geração de dados falsos uma única vez[span_19](start_span)[span_19](end_span).

### 2.3. Migrações do Room Database e Histórico de Esquemas
*   **Problema:** A configuração do Room no `AppDatabase` utiliza `exportSchema = false`[span_20](start_span)[span_20](end_span). Historicamente, o uso de `fallbackToDestructiveMigration()` em aplicações similares causa perda total de dados se o usuário pular uma versão não coberta[span_21](start_span)[span_21](end_span).
*   **Solução:** Ativar a exportação de esquemas (`exportSchema = true`) para manter um controle de versão do banco de dados[span_22](start_span)[span_22](end_span). Todas as atualizações de banco de dados (`Migrations`) devem ser explícitas para preservar os históricos e logs dos usuários[span_23](start_span)[span_23](end_span).

---

## 3. Ciclo de Vida e Processamento em Segundo Plano

### 3.1. Lembretes e Alertas Acoplados à Interface
*   **Problema:** A verificação de hábitos em atraso e a exibição de alarmes utilizam `LaunchedEffect` associados a um laço de repetição (`delay(15000)`) na camada de apresentação (Compose)[span_24](start_span)[span_24](end_span). Isso significa que os lembretes falharão se o aplicativo for fechado ou estiver em segundo plano[span_25](start_span)[span_25](end_span).
*   **Solução:** Desacoplar a lógica de alertas da interface[span_26](start_span)[span_26](end_span). Para disparos em background consistentes, deve-se utilizar o `AlarmManager` para horários exatos ou o `WorkManager` para verificações periódicas adiáveis[span_27](start_span)[span_27](end_span).

### 3.2. Congelamento da Data Corrente (Meia-noite)
*   **Problema:** A propriedade da data operacional (`todayDateStr`) no `MainViewModel` pode ser inicializada uma única vez[span_28](start_span)[span_28](end_span). Caso o app seja mantido em memória e atravesse a meia-noite, a interface continuará registrando logs e consultando relatórios como se ainda fosse o dia anterior[span_29](start_span)[span_29](end_span).
*   **Solução:** Transformar a data atual em um fluxo reativo (`StateFlow`) que atualize todos os assinantes quando ocorrer a mudança de dia, seja por tempo decorrido ou no retorno do app ao primeiro plano[span_30](start_span)[span_30](end_span).

### 3.3. Atualização e Execução do Widget
*   **Problema:** O widget dispara atualizações assíncronas no banco de dados utilizando `CoroutineScope(Dispatchers.IO).launch` sem se atrelar ao tempo de vida restrito do Broadcast Receiver, o que pode causar o cancelamento do processo pela metade pelo sistema operacional[span_31](start_span)[span_31](end_span). Falhas de banco são tratadas com blocos `catch` vazios ou apenas _logs_, ocultando erros do usuário[span_32](start_span)[span_32](end_span).
*   **Solução:** Garantir a finalização das tarefas no Provider do Widget usando `goAsync()` para segurar o ciclo de vida da Thread[span_33](start_span)[span_33](end_span). Adicionar tratamentos visuais no Widget para informar o usuário caso os dados não possam ser carregados[span_34](start_span)[span_34](end_span).

---

## Conclusão e Próximos Passos
O aplicativo possui uma boa base utilizando arquitetura MVVM, mas sofre com falhas de integridade em background e imprecisões analíticas[span_35](start_span)[span_35](end_span). Sugere-se:
1. Priorizar imediatamente as correções de **segurança do PIN** e **proteção de carregamento assíncrono**[span_36](start_span)[span_36](end_span).
2. Refatorar as fórmulas matemáticas de **adesão semanal**[span_37](start_span)[span_37](end_span).
3. Transferir as chamadas de alarme da camada visual para **WorkManager/AlarmManager**[span_38](start_span)[span_38](end_span).
