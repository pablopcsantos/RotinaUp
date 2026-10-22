Relatório Técnico de Auditoria — RotinaUp

Repositório: https://github.com/pablopcsantos/RotinaUp
Branch analisada: "main"
Referência da versão analisada: commit ""71072f27ef847b96b311c585e6c378220ed9b6ec"" (https://github.com/pablopcsantos/RotinaUp/commit/71072f27ef847b96b311c585e6c378220ed9b6ec)
Data: 21 de outubro de 2026, horário local do relatório
Tipo de análise: inspeção estática do código-fonte, configuração do CI e resultados disponíveis no GitHub Actions.

---

1. Resumo executivo

O RotinaUp é um aplicativo Android desenvolvido em Kotlin, com interface Jetpack Compose, banco de dados Room, autenticação por PIN, lembretes, widget, relatórios semanais e medalhas de progresso.

A análise identificou problemas de prioridade elevada, riscos que precisam ser validados e melhorias recomendadas.

O achado mais urgente é uma falha concreta na infraestrutura de compilação: a execução mais recente do GitHub Actions falhou porque o repositório não contém o script "gradlew" esperado pelo workflow. Consequentemente, os testes não chegaram a ser executados e a compilação do APK foi ignorada.

Também foi identificada uma inconsistência concreta no relatório semanal: a suficiência dos dados é calculada com critérios diferentes dentro da mesma função. Isso pode fazer o aplicativo indicar que existem dados suficientes quando não há hábitos ativos ou verificações programadas em quantidade adequada.

A implementação de segurança já utiliza PBKDF2, salt aleatório e bloqueio persistente após tentativas inválidas. Essas medidas são positivas, mas ainda precisam de uma política de parâmetros bem definida, validação dos cenários de migração e revisão do risco de armazenar hashes de PIN em um banco que pode ser incluído no backup.

Prioridades

Prioridade| Problema| Situação
P0 — bloqueador| CI não encontra "gradlew"| Falha confirmada
P1 — alta| Critérios inconsistentes de suficiência dos relatórios| Defeito identificado no código
P1 — alta| Segurança do PIN e exposição potencial de hashes em backups| Risco que exige correção e validação
P1 — alta| Ausência de comprovação de que testes e APK compilam| Consequência confirmada da falha no CI
P2 — média| Critérios de medalhas e elegibilidade dos registros| Necessita revisão e testes
P2 — média| Execução de consultas do banco durante a atualização do widget| Melhoria recomendada
P2 — média| Testes de migração do banco e preservação dos dados| Cobertura precisa ser confirmada
P3 — melhoria| Atualização de dependências e documentação técnica| Recomendado

Limitação: a análise do código e do histórico do CI não equivale a executar o aplicativo em um dispositivo físico. Os problemas identificados diretamente no código são distinguidos dos riscos que precisam de reprodução.

---

2. Falha P0 — O GitHub Actions não consegue executar o Gradle

Arquivos envolvidos:

- "".github/workflows/android.yml"" (https://github.com/pablopcsantos/RotinaUp/blob/main/.github/workflows/android.yml)
- "Configuração do Gradle" (https://github.com/pablopcsantos/RotinaUp/tree/main/gradle)

Execução que falhou: "GitHub Actions — Android CI" (https://github.com/pablopcsantos/RotinaUp/actions/runs/39703325934)

2.1. Evidência encontrada

O workflow tenta executar:

chmod +x gradlew || true
./gradlew :app:testDebugUnitTest --no-daemon

Entretanto, a execução registrou:

chmod: cannot access 'gradlew': No such file or directory
./gradlew: No such file or directory

O passo de testes terminou com código de saída "127". O passo de compilação do APK foi ignorado porque o teste anterior falhou.

A configuração contém referências ao wrapper do Gradle, mas o script executável "gradlew" não está presente na raiz da versão analisada.

2.2. Por que isso é importante

O CI é a principal barreira automatizada para detectar regressões antes de integrar alterações. Sem conseguir invocar o Gradle, o workflow não comprova que:

- O projeto compila;
- Os testes unitários passam;
- As alterações recentes na segurança são válidas;
- O APK pode ser construído.

A falha atual não comprova que os testes de código falham. Ela comprova que os testes nem sequer começaram.

2.3. Solução recomendada

1. Gerar ou recuperar o Gradle Wrapper compatível com a versão de Gradle usada pelo projeto.
2. Incluir no repositório os arquivos necessários, normalmente:
   - "gradlew";
   - "gradlew.bat";
   - "gradle/wrapper/gradle-wrapper.jar";
   - "gradle/wrapper/gradle-wrapper.properties".
3. Confirmar que "gradlew" está na raiz e tem permissão de execução no Git.
4. Remover o "|| true" do passo de permissões, pois ele pode esconder erros reais.
5. Executar o workflow novamente e verificar os resultados.

Em um ambiente local que já tenha uma instalação compatível do Gradle, o wrapper pode ser gerado com:

gradle wrapper

A versão usada precisa corresponder à configuração do projeto. Depois, os arquivos gerados devem ser revisados, adicionados ao Git e enviados ao repositório.

2.4. Testes de aceitação

- O comando "./gradlew --version" funciona no runner do GitHub Actions.
- "./gradlew :app:testDebugUnitTest --no-daemon" inicia e termina com sucesso.
- "./gradlew assembleDebug --no-daemon" gera o APK.
- O workflow termina com conclusão "success".

Critério de encerramento: a compilação e os testes precisam ser executados com sucesso no CI antes de considerar as demais correções validadas.

---

3. Defeito P1 — O relatório semanal utiliza dois critérios diferentes para dados suficientes

Arquivo: ""MainViewModel.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt)
Função: "computeWeeklyReport()"

3.1. O que foi encontrado

A função calcula uma variável local "hasEnoughData" usando três condições:

- Pelo menos cinco dias com dados;
- Existência de hábitos ativos;
- Pelo menos cinco verificações programadas.

Entretanto, ao montar o objeto final "WeeklyReportData", o código utiliza somente:

hasEnoughData = daysWithData >= 5

Portanto, a função pode calcular que os dados são insuficientes e, em seguida, devolver um resultado que os classifica como suficientes.

3.2. Consequências

A interface pode apresentar mensagens inconsistentes. Medalhas, recomendações e outros indicadores que dependem do resultado final também podem interpretar incorretamente a qualidade dos dados.

Além disso, a contagem "daysWithData" considera a existência de registros de hábitos ou de sono, mas isso não significa necessariamente que os registros sejam válidos para todos os indicadores. Por exemplo, a contagem geral não utiliza exatamente o mesmo filtro de duração positiva aplicado à média semanal de sono.

3.3. Solução recomendada

1. Definir uma única regra de suficiência dos dados.
2. Calcular a variável uma única vez.
3. Utilizar o mesmo resultado em todas as partes do relatório.
4. Separar a existência de registros da elegibilidade de cada métrica.
5. Definir quais tipos de registros são suficientes para cada conclusão.

Exemplo conceitual:

val hasEnoughData =
    daysWithData >= 5 &&
    activeHabitsList.isNotEmpty() &&
    totalScheduledChecks >= 5

O exemplo deve ser adaptado à regra de negócio definitiva. O ponto principal é devolver a mesma variável calculada, em vez de reavaliar apenas uma das condições.

3.4. Testes necessários

- Nenhum registro na semana;
- Cinco dias de sono registrados, sem hábitos ativos;
- Cinco dias com registros, mas menos de cinco verificações programadas;
- Registros de sono com duração zero ou negativa;
- Hábitos criados no meio da semana;
- Dados suficientes com adesão de 0%, 50% e 100%.

Critério de encerramento: a mensagem de suficiência, a recomendação, a classificação e os dados devolvidos devem ser coerentes em todos os cenários.

---

4. Segurança P1 — Reforçar a política de proteção do PIN

Arquivos:

- ""SecurityUtils.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/util/SecurityUtils.kt)
- ""UserSettings.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/model/UserSettings.kt)
- ""MainViewModel.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt)

4.1. Medidas positivas já presentes

A versão analisada já contém:

- "PBKDF2WithHmacSHA256";
- Salt aleatório de 16 bytes;
- Derivação de chave com 100.000 iterações como valor padrão;
- Comparação de hashes por "MessageDigest.isEqual";
- Verificação de limites para o número de iterações;
- Migração de credenciais antigas após autenticação válida;
- Limitação persistente de tentativas inválidas.

Esses mecanismos são melhorias relevantes e devem ser preservados.

4.2. Risco residual: PINs têm pouca entropia

Mesmo com PBKDF2, um PIN numérico curto possui um espaço de busca limitado. Se um atacante obtiver uma cópia do banco e conseguir testar hashes offline, a limitação de tentativas aplicada pela interface não impedirá a tentativa de adivinhar o PIN fora do aplicativo.

O valor de 100.000 iterações é melhor do que um hash rápido, mas não deve ser tratado como garantia absoluta de segurança. O custo precisa ser avaliado à luz das recomendações atuais e do desempenho dos dispositivos suportados.

4.3. Solução recomendada

1. Documentar o tamanho mínimo do PIN e a política de tentativas.
2. Medir o custo de PBKDF2 nos dispositivos de referência.
3. Definir parâmetros de derivação atualizáveis e registrados junto ao hash.
4. Planejar a atualização de parâmetros antigos após autenticação válida.
5. Considerar o uso de uma chave protegida pelo Android Keystore para proteger material sensível, conforme a arquitetura escolhida.
6. Não considerar o bloqueio visual como proteção suficiente contra acesso offline ao banco.

4.4. Atenção ao limite mínimo aceito

O código declara:

const val MIN_SAFE_ITERATIONS = 1000
const val MAX_SAFE_ITERATIONS = 500000
const val PBKDF2_ITERATIONS = 100000

O valor mínimo operacional permite verificar hashes com um custo muito inferior ao padrão atual. O método "needsUpgrade()" identifica hashes abaixo de 100.000 iterações para atualização posterior, mas a verificação de compatibilidade ainda permite que esses hashes sejam utilizados para autenticar o usuário.

Isso pode ser necessário para a migração, mas a política deve deixar claro que o limite de leitura não equivale ao nível de segurança recomendado para novas credenciais.

Solução: separar explicitamente os parâmetros aceitos para compatibilidade dos parâmetros exigidos para novas credenciais e atualizar hashes antigos após autenticação válida.

4.5. Testes necessários

- PIN correto e incorreto;
- Hash PBKDF2 válido;
- Hash com iterações antigas;
- Hash com iterações fora dos limites;
- Hash malformado;
- Salt ausente;
- PIN legado em texto simples;
- Hash SHA-256 legado;
- Migração concluída sem perda de configurações.

Critério de encerramento: credenciais novas utilizam a política atual, e credenciais antigas são migradas de forma segura e testada.

---

5. Segurança P1 — Revisar os dados de autenticação incluídos no backup

Arquivos:

- ""AndroidManifest.xml"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/AndroidManifest.xml)
- ""backup_rules.xml"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/res/xml/backup_rules.xml)
- ""data_extraction_rules.xml"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/res/xml/data_extraction_rules.xml)

5.1. O que foi encontrado

O manifesto define:

android:allowBackup="true"

As regras de backup incluem o banco de dados "rotinaup.db" e seus arquivos auxiliares, enquanto excluem as preferências usadas para o bloqueio por tentativas.

Como as configurações do usuário e os hashes do PIN ficam no banco, a cópia pode incluir material de autenticação.

5.2. Por que isso importa

Excluir os contadores de tentativas do backup é positivo, mas não elimina o risco de alguém obter um hash do PIN por meio de uma cópia do banco.

A política de backup precisa considerar a diferença entre:

- Preservar hábitos e registros pessoais;
- Preservar configurações;
- Preservar credenciais de autenticação;
- Transferir dados para outro dispositivo.

5.3. Solução recomendada

1. Decidir formalmente quais dados podem ser copiados.
2. Avaliar se as credenciais de autenticação devem ser restauradas junto com o banco.
3. Considerar desativar o backup de dados sensíveis ou separar credenciais e dados de rotina.
4. Se a restauração de hábitos for necessária, planejar uma forma de restaurar os dados sem restaurar automaticamente uma credencial vulnerável.
5. Documentar o comportamento após reinstalação, restauração e transferência de dispositivo.

Não basta excluir o arquivo inteiro se isso eliminar uma funcionalidade desejada. A solução deve equilibrar privacidade e recuperação dos dados.

5.4. Testes necessários

- Backup e restauração;
- Reinstalação do aplicativo;
- Transferência para outro dispositivo;
- Configuração com PIN ativado;
- Configuração sem PIN;
- Confirmação de que o fluxo de restauração não permite contornar a autenticação.

Critério de encerramento: o conteúdo restaurado corresponde à política de privacidade definida, sem enfraquecer a proteção do PIN.

---

6. Segurança P1 — Verificar a migração de credenciais antigas

Arquivos:

- ""AppDatabase.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/db/AppDatabase.kt)
- ""UserSettings.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/model/UserSettings.kt)
- ""SecurityUtils.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/util/SecurityUtils.kt)

6.1. O que foi encontrado

A entidade ainda mantém "pinCode", além de "pinHash" e "pinSalt". O código conserva compatibilidade com PIN em texto simples e hash legado.

A migração Room 5→6 adiciona as colunas "pinHash" e "pinSalt", mas não remove a coluna "pinCode".

A presença da coluna antiga, isoladamente, não prova que existe uma vulnerabilidade ativa: o risco depende de os valores legados ainda estarem armazenados e de como a atualização é executada.

6.2. Solução recomendada

1. Documentar todos os formatos legados suportados.
2. Garantir que "needsUpgrade()" identifique corretamente cada formato antigo.
3. Testar a migração após autenticação válida.
4. Confirmar que "createHashedSettings()" apaga o PIN em texto simples.
5. Planejar uma migração de esquema que remova a coluna antiga quando for compatível com a política de versões suportadas.
6. Garantir que uma migração interrompida possa ser repetida sem corromper os dados.

6.3. Testes necessários

- Banco antigo com PIN em texto simples;
- Banco antigo com SHA-256;
- Banco com PBKDF2 com parâmetros antigos;
- Banco com PBKDF2 atual;
- Banco com autenticação desativada;
- Atualização de cada versão histórica suportada;
- Verificação do banco depois da migração.

Critério de encerramento: credenciais antigas são migradas sem perda de acesso, sem armazenamento persistente desnecessário do PIN e sem regressão da autenticação.

---

7. Lógica P2 — Revisar as condições de conquista das medalhas

Arquivo: ""MainViewModel.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt)

7.1. O que foi encontrado

As medalhas possuem critérios diferentes. Algumas utilizam "hasEnoughData"; outras usam contagens de sono, sequência de hábitos ou registros de conclusão.

Essa diferença pode ser válida quando cada medalha possui requisitos próprios. O ponto que precisa ser comprovado é se cada algoritmo filtra corretamente os dados utilizados.

Por exemplo, uma contagem de hábitos concluídos precisa considerar a semana correta, os dias programados e o hábito ao qual cada registro pertence.

7.2. Solução recomendada

Para cada medalha, documentar:

- Período avaliado;
- Dados obrigatórios;
- Critérios de elegibilidade;
- Fórmula de progresso;
- Condição exata de desbloqueio;
- Mensagem mostrada quando não existem dados suficientes.

Criar funções de cálculo que possam ser testadas independentemente da interface.

7.3. Testes necessários

- Dados insuficientes;
- Registros fora da semana;
- Hábitos inativos;
- Dias em que o hábito não estava programado;
- Hábito criado no meio da semana;
- Limite exato de conquista;
- Registros ausentes e duplicados.

Critério de encerramento: cada medalha deve ser concedida somente quando todos os requisitos documentados estiverem comprovados.

---

8. Widget P2 — Reduzir o risco de bloqueios durante a atualização

Arquivo: ""RotinaUpWidgetProvider.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/widget/RotinaUpWidgetProvider.kt)

8.1. O que foi encontrado

O widget utiliza "goAsync()" para continuar o trabalho depois do retorno do broadcast, executa operações em um dispatcher de IO e chama "finish()" no bloco "finally". Esse padrão é positivo.

Entretanto, a função de atualização realiza consultas ao banco utilizando "runBlocking" e faz múltiplas leituras separadas para obter configurações, hábitos e registros.

Mesmo com a execução em IO nos caminhos observados, essa abordagem pode tornar a atualização mais lenta e difícil de testar. Também exige cuidado com o limite de execução dos broadcasts do Android.

8.2. Solução recomendada

1. Transformar a atualização em uma função "suspend" quando possível.
2. Executar as consultas diretamente dentro da coroutine, sem criar bloqueios desnecessários.
3. Reduzir o número de consultas e obter os dados necessários de maneira consistente.
4. Manter o tratamento de exceções e garantir que "finish()" seja chamado em todos os caminhos.
5. Testar a atualização com banco vazio, banco populado e falha de leitura.

8.3. Testes necessários

- Atualização normal;
- Banco sem hábitos;
- Alteração de hábito seguida de atualização;
- Falha na leitura do banco;
- Vários widgets instalados;
- Aplicativo em segundo plano;
- Autenticação ativada.

Critério de encerramento: o widget deve atualizar de forma confiável, sem atrasos excessivos ou exibição de informações que deveriam permanecer protegidas.

---

9. Banco de dados P2 — Completar os testes de migração Room

Arquivo: ""AppDatabase.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/db/AppDatabase.kt)

9.1. O que foi encontrado

O banco utiliza migrações explícitas e declara "exportSchema = true". A versão atual do esquema é 7, e há migrações entre as versões 1 e 7.

Isso é uma boa prática. O problema não é a ausência de migrações, mas a necessidade de demonstrar que todas as migrações históricas funcionam com dados reais de versões anteriores.

9.2. Solução recomendada

1. Criar testes de migração para todas as versões que precisam ser suportadas.
2. Verificar a estrutura do banco após cada migração.
3. Confirmar que os registros existentes permanecem intactos.
4. Validar valores nulos e valores padrão adicionados por migrações.
5. Garantir que os arquivos de esquema versionados correspondam ao código.
6. Nunca resolver falhas de atualização destruindo silenciosamente o banco do usuário.

9.3. Testes necessários

Verificar a preservação de:

- Hábitos;
- Registros de hábitos;
- Registros de sono;
- Cochilos;
- Avaliações diárias;
- Configurações;
- Eventos de alarme;
- Dados de autenticação durante a migração.

Critério de encerramento: todas as versões suportadas atualizam o banco sem perda de dados e sem incompatibilidades de esquema.

---

10. Testes P1 — A cobertura só pode ser considerada validada depois da execução

Diretórios relevantes:

- ""app/src/test"" (https://github.com/pablopcsantos/RotinaUp/tree/main/app/src/test)
- ""app/src/androidTest"" (https://github.com/pablopcsantos/RotinaUp/tree/main/app/src/androidTest)

10.1. Situação encontrada

O repositório possui testes para segurança, relatórios, datas e operações de banco. Isso é positivo.

Porém, a execução mais recente do CI não chegou a executar os testes por causa da ausência do script "gradlew".

Portanto, não há base para afirmar que a suíte atual passa na versão analisada.

10.2. Solução recomendada

Depois de corrigir o wrapper:

1. Executar todos os testes unitários.
2. Corrigir falhas de compilação e de testes.
3. Acrescentar testes para as inconsistências encontradas nesta auditoria.
4. Executar testes instrumentados nos fluxos que dependem do Android.
5. Reexecutar o CI depois de cada alteração crítica.
6. Registrar o commit testado e os resultados.

10.3. Cobertura mínima recomendada

- Autenticação inicial;
- Bloqueio após tentativas incorretas;
- Migração de credenciais;
- Relatórios com dados insuficientes;
- Medalhas com registros inválidos;
- Migrações Room;
- Backup e restauração;
- Atualização do widget;
- Alarmes e permissões de notificação.

Critério de encerramento: os testes devem passar no CI e os fluxos dependentes do Android devem ser verificados em ambiente instrumentado ou dispositivo de referência.

---

11. Lembretes P2 — Validar o comportamento no Android real

Arquivos relacionados:

- ""AndroidManifest.xml"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/AndroidManifest.xml)
- "Receptor de alarmes" (https://github.com/pablopcsantos/RotinaUp/tree/main/app/src/main/java/com/example/receiver)

11.1. O que foi encontrado

O manifesto declara permissões para notificações, alarmes exatos e inicialização após o boot. O projeto também possui um receptor de alarmes.

A presença dessas declarações não comprova, por si só, que todos os lembretes são entregues nos horários esperados.

11.2. Solução recomendada

Testar a entrega dos lembretes:

- Com o aplicativo aberto;
- Em segundo plano;
- Após encerramento do processo;
- Depois da reinicialização do aparelho;
- Depois de mudanças de fuso horário;
- Com permissão de notificações negada;
- Com permissão de alarmes exatos indisponível.

Confirmar que alarmes antigos são cancelados quando os horários mudam e que não há duplicação de notificações.

Critério de encerramento: o comportamento de criação, cancelamento e reagendamento deve ser previsível e compatível com as permissões disponíveis.

---

12. CI P2 — Melhorar o workflow depois de restabelecer a compilação

Arquivo: "".github/workflows/android.yml"" (https://github.com/pablopcsantos/RotinaUp/blob/main/.github/workflows/android.yml)

12.1. O que foi encontrado

O workflow configura o JDK 21, executa testes unitários e tenta construir o APK. A estrutura básica é adequada.

A execução recente também apresentou um aviso de que "actions/setup-java@v4" está depreciado e não receberá novas atualizações.

Esse aviso não foi a causa da falha atual; a causa foi a ausência de "gradlew".

12.2. Solução recomendada

1. Corrigir a ausência do wrapper.
2. Confirmar a compatibilidade entre JDK, Gradle e Android Gradle Plugin.
3. Atualizar as actions para versões suportadas após verificar a compatibilidade.
4. Remover comandos que escondem falhas, como "chmod +x gradlew || true".
5. Considerar relatórios de testes como artefatos do workflow.
6. Fazer o CI bloquear alterações quando a compilação ou os testes falharem.

Critério de encerramento: o workflow deve terminar com sucesso e apresentar evidências verificáveis da execução dos testes e da compilação.

---

13. Documentação P3 — Atualizar comentários e contratos técnicos

Arquivo: ""UserSettings.kt"" (https://github.com/pablopcsantos/RotinaUp/blob/main/app/src/main/java/com/example/data/model/UserSettings.kt)

O que foi encontrado

O comentário da propriedade "pinHash" ainda a descreve como hash SHA-256 com salt, embora a implementação atual utilize PBKDF2 para credenciais modernas e mantenha SHA-256 para compatibilidade com versões antigas.

Isso não é, por si só, um defeito funcional, mas pode induzir manutenção incorreta.

Solução recomendada

1. Atualizar comentários para descrever o formato atual e os formatos legados.
2. Documentar os parâmetros de derivação.
3. Descrever o fluxo de migração.
4. Documentar a política de backup e autenticação.
5. Manter as instruções de execução do projeto alinhadas com o Gradle Wrapper efetivamente versionado.

Critério de encerramento: a documentação deve refletir a implementação real e não induzir decisões de manutenção equivocadas.

---

14. Plano de execução recomendado

Fase 1 — Restabelecer a capacidade de validar o projeto

1. Recuperar o Gradle Wrapper.
2. Corrigir o workflow.
3. Executar os testes unitários.
4. Executar a compilação do APK.
5. Registrar as falhas encontradas.

Fase 2 — Corrigir os defeitos de lógica e segurança

1. Unificar o critério "hasEnoughData".
2. Corrigir e testar os filtros de elegibilidade dos relatórios.
3. Revisar os critérios de medalhas.
4. Revisar a política PBKDF2 e a migração de credenciais.
5. Definir a política de backup das credenciais.

Fase 3 — Validar a integração com o Android

1. Testar as migrações Room.
2. Testar widget e consultas ao banco.
3. Testar notificações e alarmes.
4. Testar restauração e transferência de dados.
5. Executar testes instrumentados.

Fase 4 — Preparar uma versão confiável

1. Atualizar a documentação.
2. Verificar o CI em um commit final.
3. Confirmar a compilação do APK.
4. Corrigir todos os testes que falharem.
5. Registrar os resultados da validação.

---

15. Matriz de aceitação final

Área| Evidência exigida| Condição de aprovação
Gradle Wrapper| "./gradlew --version"| Executa corretamente
Testes unitários| Resultado do CI| Todos os testes passam
Compilação| "assembleDebug"| APK gerado com sucesso
Relatórios| Testes de dados insuficientes| Resultados coerentes
Medalhas| Testes de elegibilidade| Nenhuma conquista indevida
PIN| Testes de hashes e migração| Autenticação correta e segura
Backup| Testes de restauração| Política de privacidade respeitada
Room| Testes de migração| Dados preservados
Widget| Testes de atualização| Conteúdo consistente e atualizado
Alarmes| Testes em Android| Entrega e reagendamento previsíveis
Documentação| Revisão de arquivos| Instruções e comentários atualizados

---

16. Conclusão

O RotinaUp já possui estruturas relevantes de segurança, persistência e testes. O principal problema operacional confirmado é a impossibilidade de executar o CI devido à ausência do "gradlew" esperado pelo workflow. Isso deve ser corrigido antes de usar a suíte de testes como evidência de qualidade.

O principal defeito funcional identificado está na inconsistência dos critérios de suficiência dos dados do relatório semanal. A segurança do PIN também merece atenção, sobretudo na política de parâmetros, migração de credenciais e inclusão do banco em backups.

A sequência recomendada é:

1. Corrigir o Gradle Wrapper e restabelecer o CI.
2. Executar os testes e a compilação para descobrir falhas adicionais.
3. Corrigir a lógica do relatório semanal e testar as medalhas.
4. Validar a migração e a política de proteção do PIN.
5. Testar banco, backup, widget e alarmes em ambientes apropriados.

Nenhuma correção deve ser considerada concluída apenas porque o código foi alterado. A conclusão depende de testes que passem e, quando necessário, de reprodução em ambiente Android.

Limitações da auditoria

- A análise do código foi estática.
- A execução mais recente do CI falhou antes de iniciar os testes.
- Não foi comprovado que o APK compila.
- Não foram executados testes completos em dispositivo físico.
- Os riscos que não possuem reprodução direta devem ser tratados como itens de validação, não como defeitos comprovados.
- Este relatório não representa uma certificação formal de segurança.

Resultado final: a prioridade imediata é recuperar a execução automatizada do projeto. Em seguida, devem ser corrigidas as inconsistências de relatórios e validados os mecanismos de segurança antes de considerar a qualidade da versão comprovada.