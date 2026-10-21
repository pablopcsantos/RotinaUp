Relatório de Auditoria Técnica — RotinaUp

Repositório: https://github.com/pablopcsantos/RotinaUp
Data da análise: 20 de outubro de 2026
Tipo de auditoria: análise estática de código, segurança, integridade dos dados, regras de negócio, testes e manutenção
Status: diagnóstico realizado; correções ainda pendentes de aplicação e validação.

---

1. Resumo executivo

A análise do RotinaUp identificou riscos relacionados à autenticação por PIN, ao carregamento assíncrono das configurações, à persistência do bloqueio, à inicialização dos dados, à precisão dos relatórios semanais e ao funcionamento dos lembretes e widgets.

O repositório também apresenta melhorias em relação à análise anterior. Entre elas estão a remoção do fallback destrutivo de migração do Room, a adoção de hash e salt para o PIN, a exclusão transacional de hábitos e registros associados e a inclusão de testes automatizados.

Essas melhorias são positivas, mas não eliminam todos os riscos.

Prioridades recomendadas

Prioridade| Problema| Consequência possível
P0 — Crítica| Estado de autenticação inicial potencialmente incorreto| Acesso ao aplicativo sem validação do PIN
P1 — Alta| Armazenamento e verificação de PIN legado| Proteção insuficiente de credenciais antigas
P1 — Alta| Bloqueio por tentativas inválidas apenas em memória| Reinicialização do limite ao recriar o processo
P1 — Alta| Regras de cálculo do relatório semanal| Percentuais e recomendações incorretos
P1 — Alta| Inicialização automática de dados demonstrativos| Reaparecimento de hábitos excluídos
P2 — Média| Execução assíncrona do widget| Atualização interrompida ou desatualizada
P2 — Média| Lembretes dependentes do ciclo de vida do aplicativo| Notificações ausentes quando o app não está ativo
P2 — Média| Política de backup indefinida| Exposição ou restauração inesperada de dados
P2 — Média| Cobertura insuficiente de cenários críticos| Regressões não detectadas
P3 — Baixa| Exportação de esquema e integração contínua| Maior dificuldade de manutenção e validação

Observação: as prioridades são uma classificação técnica preliminar. A gravidade efetiva deve ser confirmada por testes de reprodução, análise das configurações de distribuição e avaliação dos requisitos do aplicativo.

---

2. Escopo e limitações

A análise considerou o código-fonte e as alterações identificadas no repositório, incluindo a revisão mais recente consultada:

https://github.com/pablopcsantos/RotinaUp/commit/0f752f685f652576b226126cf6516307dde10d6d

Foram avaliados os seguintes aspectos:

- autenticação e proteção por PIN;
- persistência e migração do banco de dados;
- regras de negócio dos relatórios;
- inicialização e exclusão de dados;
- lembretes e widgets;
- testes automatizados;
- documentação e manutenção.

Não foi possível afirmar que o projeto compila ou que os testes passam, pois esta análise não inclui uma execução integral do build Android, testes instrumentados em dispositivo ou validação de todos os fluxos em tempo de execução.

As ocorrências descritas como riscos devem ser confirmadas antes de serem consideradas reproduzidas em produção.

---

3. Falhas e riscos identificados

3.1. P0 — Possível bypass da autenticação durante a inicialização

Área: "MainActivity.kt" e fluxo de carregamento das configurações.

Problema identificado

O estado inicial de desbloqueio é calculado a partir de configurações que podem ser carregadas de forma assíncrona. O fluxo observado utiliza uma lógica equivalente a:

var isUnlocked by remember {
    mutableStateOf(!settings.requireAuth || !settings.hasPin())
}

Se "settings" começar com valores padrão antes de o banco de dados emitir as configurações persistidas, o estado poderá ser inicializado como desbloqueado. Como "remember" não recalcula automaticamente seu valor quando as configurações mudam, o estado inicial pode continuar válido depois que o PIN real for carregado.

O risco depende do fluxo completo de autenticação e deve ser confirmado por um teste de reprodução.

Consequência possível

O aplicativo pode permitir acesso à interface protegida sem exigir o PIN configurado.

Solução recomendada

1. Representar o carregamento das configurações como um estado explícito.
2. Não permitir acesso às telas protegidas enquanto a configuração de autenticação não tiver sido carregada.
3. Calcular a decisão de autenticação com base nas configurações persistidas, não em valores provisórios.
4. Garantir que alterações no PIN e na exigência de autenticação sejam refletidas corretamente.
5. Evitar que a tela protegida seja exibida antes da resolução do estado de autenticação.

Modelo conceitual:

sealed interface AuthInitializationState {
    data object Loading : AuthInitializationState
    data object Unlocked : AuthInitializationState
    data object Locked : AuthInitializationState
}

A implementação final deve respeitar a arquitetura já utilizada pelo aplicativo.

Testes obrigatórios

- Abrir o aplicativo com PIN previamente configurado.
- Simular carregamento lento das configurações.
- Confirmar que nenhuma tela protegida fica acessível durante o carregamento.
- Confirmar que o PIN continua obrigatório após recriação da Activity.
- Confirmar que o comportamento permanece correto após reiniciar o processo.

Critério de aceitação: nenhuma configuração provisória pode ser interpretada como autorização para entrar no aplicativo.

---

3.2. P1 — Hash do PIN inadequado para um segredo de baixa entropia

Área: "SecurityUtils.kt".

Problema identificado

A implementação utiliza SHA-256 com salt para proteger o PIN. Isso é melhor do que armazenar diretamente o segredo, mas SHA-256 é uma função de hash rápida.

PINs numéricos curtos possuem um espaço de busca pequeno. Caso alguém obtenha o hash e o salt, poderá testar combinações rapidamente.

Consequência possível

Um PIN de quatro ou seis dígitos pode ser descoberto por tentativa offline se o armazenamento protegido for extraído.

Solução recomendada

1. Substituir o esquema rápido por uma função de derivação de chave apropriada para senhas, como PBKDF2, com parâmetros adequados à plataforma, ou outra solução consolidada e compatível com o projeto.
2. Utilizar salt aleatório e exclusivo por credencial.
3. Armazenar a identificação do algoritmo e seus parâmetros para permitir futuras atualizações.
4. Fazer comparação segura dos resultados quando a API escolhida permitir.
5. Planejar a migração dos hashes existentes sem invalidar silenciosamente as credenciais dos usuários.

Importante: uma função de derivação mais lenta dificulta ataques offline, mas não substitui o bloqueio de tentativas online.

Testes obrigatórios

- PIN correto e incorreto.
- Salt diferente para credenciais distintas.
- Migração de hash antigo para o algoritmo novo.
- Tratamento de dados persistidos incompletos ou incompatíveis.

Critério de aceitação: novas credenciais não devem utilizar apenas um hash rápido e genérico.

---

3.3. P1 — PIN legado ainda armazenado e aceito

Área: "UserSettings.kt", migração do banco de dados e verificação de PIN.

Problema identificado

A implementação ainda mantém um campo legado equivalente a "pinCode: String?". A migração que introduz "pinHash" e "pinSalt" não elimina o campo antigo, e o código ainda permite validar credenciais legadas em texto simples antes de convertê-las após autenticação bem-sucedida.

Essa compatibilidade pode ser necessária para usuários antigos, mas precisa ser temporária e controlada.

Consequência possível

O PIN antigo pode continuar exposto no armazenamento persistente, mesmo após a introdução do mecanismo de hash.

Solução recomendada

1. Criar uma migração segura para a estrutura de credenciais.
2. Definir uma estratégia explícita para usuários que ainda possuem PIN em texto simples.
3. Converter a credencial para o formato moderno após validação legítima.
4. Remover o campo legado quando a migração for concluída e compatível com o modelo de dados.
5. Não apagar ou invalidar credenciais antes de garantir que os usuários consigam autenticar-se.
6. Documentar a estratégia de compatibilidade e a versão em que o legado será removido.

Testes obrigatórios

- Instalação limpa.
- Atualização a partir de cada versão de banco relevante.
- Usuário com PIN legado válido.
- Usuário com PIN legado inválido.
- Reinicialização após conversão da credencial.
- Verificação de que o PIN antigo não permanece acessível após a migração final.

Critério de aceitação: o suporte a PIN em texto simples não deve permanecer indefinidamente nem deixar credenciais antigas expostas sem necessidade.

---

3.4. P1 — Limite de tentativas inválidas não persiste à recriação do processo

Área: "AuthPinLockScreen.kt".

Problema identificado

Os contadores de tentativas inválidas e o tempo de bloqueio utilizam estado em memória, com "remember". Isso significa que recriar a tela ou o processo pode reinicializar esses valores.

Consequência possível

O usuário pode conseguir reiniciar o contador de tentativas ao fechar, reabrir ou provocar a recriação do aplicativo.

Solução recomendada

1. Centralizar a política de tentativas em um componente de autenticação.
2. Persistir os dados necessários para aplicar o bloqueio através de recriações do processo.
3. Usar relógio monotônico para medir intervalos durante a execução.
4. Ao persistir o bloqueio entre reinicializações, definir uma estratégia segura para a referência temporal.
5. Evitar bloqueios permanentes causados por estados inconsistentes ou relógios do sistema alterados.
6. Limitar a frequência das tentativas sem tornar a recuperação legítima excessivamente difícil.

A persistência pode utilizar DataStore ou outra solução já adotada no projeto. O mecanismo deve ser consistente com o modelo de segurança pretendido.

Testes obrigatórios

- Exceder o limite de tentativas.
- Recriar a tela.
- Reiniciar o processo.
- Alterar a hora do dispositivo.
- Autenticar corretamente após o período de bloqueio.
- Confirmar que tentativas válidas e inválidas atualizam o estado corretamente.

Critério de aceitação: a simples recriação da interface não deve zerar a política de bloqueio.

---

3.5. P1 — Relatório semanal pode calcular a adesão incorretamente

Área: "MainViewModel.kt", especialmente "computeWeeklyReport".

Problema identificado

A lógica observada apresenta riscos em três pontos:

1. O denominador pode incluir dias anteriores à criação do hábito.
2. O numerador pode contabilizar registros "DONE" ou "PARTIAL" em dias nos quais o hábito não estava programado.
3. O total de registros pode ser somado sem a mesma verificação de elegibilidade usada para calcular os dias previstos.

A aplicação de "coerceIn(0, 100)" pode esconder percentuais incoerentes em vez de corrigir a origem do cálculo.

Consequência possível

O relatório pode apresentar taxas de adesão, medalhas ou sugestões que não correspondem ao comportamento real do usuário.

Solução recomendada

Definir uma única regra de elegibilidade por hábito e por data.

Um dia deve ser incluído no cálculo apenas quando:

- está dentro do período do relatório;
- não é anterior à criação do hábito;
- corresponde a um dia programado para aquele hábito;
- pertence ao calendário e fuso horário utilizados pelo aplicativo.

Os registros realizados fora dos dias previstos não devem aumentar a adesão programada, a menos que o produto defina explicitamente outra regra.

Modelo conceitual:

diasPrevistos =
    datas elegíveis em que o hábito estava programado

diasConcluidos =
    datas elegíveis com conclusão válida

adesao =
    diasConcluidos / diasPrevistos * 100

Quando "diasPrevistos" for zero, a taxa deve ser tratada como ausência de dados, não como uma adesão real de 0%.

Testes obrigatórios

- Hábito criado no meio da semana.
- Hábito programado apenas em dias úteis.
- Registro de conclusão em dia não programado.
- Registro parcial.
- Semana sem hábitos previstos.
- Semana com registros incompletos.
- Datas próximas à mudança de dia local.

Critério de aceitação: o numerador e o denominador devem utilizar exatamente o mesmo conjunto de datas elegíveis.

---

3.6. P1 — Medalhas e sugestões podem aparecer sem dados suficientes

Área: geração de medalhas e recomendações do relatório semanal.

Problema identificado

A existência de uma variável como "hasEnoughData" não garante que todas as medalhas e sugestões respeitem essa condição.

A lógica observada pode calcular resultados com base em poucos registros. Também há risco de apresentar uma mensagem positiva de consistência quando não existem hábitos previstos ou quando não há dados suficientes para avaliar o desempenho.

Consequência possível

O aplicativo pode gerar feedback enganoso, prejudicando a confiança do usuário nos relatórios.

Solução recomendada

1. Definir critérios mínimos de evidência para cada medalha.
2. Separar “sem dados”, “dados insuficientes” e “desempenho avaliado”.
3. Não emitir recomendações de desempenho quando não houver base suficiente.
4. Distinguir ausência de registros de falha de adesão.
5. Testar todas as condições de fronteira.
6. Garantir que as mensagens reflitam os valores efetivamente calculados.

Testes obrigatórios

- Nenhum hábito cadastrado.
- Hábitos cadastrados, mas sem dias previstos.
- Menos de cinco dias com dados.
- Dados suficientes para avaliar o desempenho.
- Todas as conclusões registradas em dias não programados.
- Nenhum registro de sono.

Critério de aceitação: medalhas e recomendações devem ser justificáveis pelos dados utilizados para calculá-las.

---

3.7. P1 — Hábitos demonstrativos podem reaparecer após exclusão

Área: inicialização de dados em "MainViewModel.kt".

Problema identificado

A inicialização consulta a quantidade de hábitos e executa "seedInitialData()" quando o total é zero. A rotina adiciona hábitos e registros demonstrativos.

Esse comportamento pode confundir uma base vazia com uma instalação nova.

Consequência possível

Se o usuário excluir todos os hábitos, os dados demonstrativos podem reaparecer na inicialização seguinte, dependendo do fluxo de inicialização.

Solução recomendada

1. Diferenciar primeira execução de banco vazio após uso normal.
2. Registrar explicitamente se a configuração inicial ou demonstração já foi concluída.
3. Executar a criação de dados de demonstração apenas quando isso for desejado e autorizado.
4. Não reintroduzir hábitos automaticamente depois de uma exclusão legítima.
5. Fazer a inicialização de forma transacional quando várias entidades precisarem ser criadas em conjunto.

Testes obrigatórios

- Primeira execução.
- Exclusão de todos os hábitos.
- Fechamento e reabertura do aplicativo.
- Atualização de versão.
- Banco vazio após restauração.
- Falha parcial durante a inicialização.

Critério de aceitação: uma base vazia não deve, por si só, provocar a reinserção automática de dados demonstrativos.

---

3.8. P2 — Semântica inconsistente para hábitos sem dias definidos

Área: cálculo do relatório semanal e widget.

Problema identificado

O relatório e o widget parecem tratar listas vazias de dias da semana de maneiras diferentes. No relatório, uma lista vazia pode ser interpretada como todos os dias; no widget, pode ser interpretada como nenhum dia.

Consequência possível

Um hábito pode aparecer como previsto no relatório e não aparecer como previsto no widget, gerando informações contraditórias.

Solução recomendada

1. Definir formalmente o significado de "daysOfWeek" vazio.
2. Criar uma única função de domínio para determinar se um hábito está programado em uma data.
3. Reutilizar a regra no relatório, na interface e no widget.
4. Tratar dados inválidos ou antigos de forma explícita.

Testes obrigatórios

- Lista vazia.
- Todos os dias.
- Apenas dias úteis.
- Um único dia da semana.
- Valores duplicados.
- Valores inválidos ou malformados.

Critério de aceitação: todas as partes do aplicativo devem interpretar a programação do hábito da mesma maneira.

---

3.9. P2 — Atualização do widget pode ser interrompida

Área: "RotinaUpWidgetProvider.kt".

Problema identificado

O uso de um "CoroutineScope(Dispatchers.IO).launch" iniciado diretamente durante a atualização do widget pode deixar trabalho assíncrono sem garantia de conclusão quando o processamento do broadcast termina ou o processo é encerrado.

O risco exato depende da operação executada e do tempo de processamento.

Consequência possível

O widget pode permanecer desatualizado ou deixar de exibir informações recentes.

Solução recomendada

1. Reduzir o trabalho realizado diretamente no callback de atualização.
2. Utilizar "goAsync()" para operações curtas que precisam continuar após o retorno do callback, encerrando o "PendingResult" em todos os caminhos.
3. Utilizar WorkManager para tarefas adiáveis que precisem de execução persistente.
4. Garantir que falhas e cancelamentos não deixem o broadcast aberto.
5. Atualizar a interface do widget apenas depois de obter os dados necessários.

Testes obrigatórios

- Atualização normal.
- Processo encerrado durante a atualização.
- Banco de dados indisponível.
- Atualizações consecutivas.
- Widget após reinicialização do dispositivo.
- Tratamento de exceções.

Critério de aceitação: o mecanismo de atualização deve respeitar o ciclo de vida do Android e o tempo de execução disponível para cada tipo de trabalho.

---

3.10. P2 — Lembretes dependem do ciclo de vida do aplicativo

Área: "MainActivity.kt" e lógica de notificações.

Problema identificado

A lógica de lembretes observada utiliza "LaunchedEffect" e uma rotina de verificação periódica de aproximadamente 15 segundos.

Esse mecanismo pode funcionar enquanto a composição e o processo permanecem ativos, mas não garante, por si só, que a verificação continue quando o aplicativo estiver em segundo plano, fechado ou recriado.

Consequência possível

Notificações podem não ser emitidas nos horários esperados.

Solução recomendada

A implementação depende do requisito de produto:

- Para lembretes em horários específicos, avaliar AlarmManager e as restrições aplicáveis do Android.
- Para tarefas adiáveis, avaliar WorkManager.
- Para lembretes locais, considerar agendamento explícito e reagendamento após reinicialização, quando necessário.
- Solicitar e verificar a permissão de notificações nas versões do Android que a exigem.
- Considerar restrições de bateria, permissões especiais e políticas de execução em segundo plano.

A seleção da API deve ser feita depois de definir se o horário precisa ser exato ou aproximado.

Testes obrigatórios

- Aplicativo aberto.
- Aplicativo em segundo plano.
- Processo encerrado.
- Dispositivo reiniciado.
- Permissão de notificações negada.
- Mudança de fuso horário.
- Mudança de horário do lembrete.
- Restrições de bateria.

Critério de aceitação: o comportamento dos lembretes deve corresponder ao requisito documentado e às limitações da plataforma.

---

3.11. P2 — Política de backup não está suficientemente definida

Área: "AndroidManifest.xml" e arquivos de regras de backup.

Problema identificado

O aplicativo utiliza "android:allowBackup="true"", enquanto os arquivos de regras de backup ainda precisam de uma revisão para garantir que representem a política desejada.

A presença de PIN e dados de rotina torna importante avaliar o que pode ser copiado ou restaurado.

Consequência possível

Dados privados podem ser incluídos em backups ou restaurados em situações não previstas, conforme as regras aplicadas e a versão do Android.

Solução recomendada

1. Definir quais dados devem participar do backup.
2. Revisar as regras para os mecanismos de backup aplicáveis às versões de Android suportadas.
3. Evitar incluir segredos ou material de autenticação sem uma justificativa de segurança.
4. Verificar a compatibilidade entre restauração e migrações do banco.
5. Documentar a política de privacidade e recuperação dos dados.

Testes obrigatórios

- Instalação limpa.
- Backup e restauração.
- Atualização de versão.
- Migração do banco após restauração.
- Verificação dos dados sensíveis incluídos no backup.

