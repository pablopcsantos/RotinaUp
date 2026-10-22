RELATÓRIO TÉCNICO DE AUDITORIA — ROTINAUP

Repositório: https://github.com/pablopcsantos/RotinaUp
Branch analisada: "main"
Data da análise: 21 de outubro de 2026
Tipo de auditoria: análise estática do código-fonte, estrutura do banco de dados, autenticação, relatórios, widget, testes e integração contínua.

---

1. RESUMO EXECUTIVO

O RotinaUp é um aplicativo Android desenvolvido em Kotlin, com interface em Jetpack Compose, persistência por Room, notificações e lembretes, relatórios semanais, medalhas e proteção opcional por PIN.

O projeto já possui uma base técnica relevante. Foram identificadas implementações positivas, incluindo migrações explícitas do banco de dados, uso de PBKDF2 para derivação do hash do PIN, controle persistente de tentativas de autenticação, testes automatizados para relatórios e segurança e um fluxo de integração contínua no GitHub Actions.

Entretanto, ainda existem pontos que merecem correção ou validação antes de considerar o aplicativo confiável para uso cotidiano.

Principais prioridades

1. Segurança: aumentar e parametrizar o custo computacional do PBKDF2, revisar a migração de credenciais antigas e testar o bloqueio de autenticação em cenários reais.
2. Relatórios: corrigir a inconsistência entre os critérios locais e os critérios efetivamente devolvidos para determinar se há dados suficientes.
3. Medalhas e recomendações: garantir que os resultados dependam de registros válidos, elegíveis e suficientes.
4. Integridade dos dados: validar importações, exportações, migrações e preservação de dados em atualizações.
5. Lembretes: testar entrega, reagendamento, permissões e comportamento após reinicialização do dispositivo.
6. Qualidade: executar a suíte de testes e a compilação, acrescentar testes para os cenários críticos e verificar os resultados do CI.

Importante: esta auditoria é baseada na leitura do código disponível no repositório. Os itens abaixo estão classificados como defeitos observados, riscos técnicos ou verificações pendentes. Não foi realizada, nesta análise, uma execução completa do aplicativo em dispositivo físico nem uma execução integral dos testes.

---

2. PROBLEMA CRÍTICO — INCONSISTÊNCIA NA VALIDAÇÃO DE DADOS DO RELATÓRIO SEMANAL

Arquivo: "app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt"

O que foi identificado

A função "computeWeeklyReport()" calcula localmente a variável "hasEnoughData" considerando três condições:

- Pelo menos cinco dias com registros;
- Existência de hábitos ativos;
- Pelo menos cinco verificações de hábitos programadas na semana.

Entretanto, ao construir o objeto "WeeklyReportData", o código devolve uma condição diferente:

"hasEnoughData = daysWithData >= 5"

Assim, o valor final do relatório pode indicar que há dados suficientes mesmo quando não existem hábitos ativos ou verificações programadas suficientes.

Esse problema é observável diretamente na implementação e pode afetar a interface e qualquer funcionalidade que utilize o valor devolvido pelo relatório.

Solução recomendada

1. Calcular uma única variável de elegibilidade dos dados.
2. Utilizar essa mesma variável na sugestão semanal, na geração das medalhas, na classificação e no objeto devolvido.
3. Diferenciar ausência de dados de adesão efetivamente igual a zero.
4. Exibir uma mensagem clara quando os dados não forem suficientes para produzir uma conclusão confiável.

Testes obrigatórios

- Nenhum registro na semana.
- Cinco dias com registros de sono, mas sem hábitos ativos.
- Cinco dias com registros, porém menos de cinco verificações elegíveis.
- Dados suficientes e adesão igual a zero.
- Dados suficientes com adesão intermediária e alta.

Critério de aceitação: todos os componentes do relatório devem concordar sobre a suficiência dos dados.

---

3. PROBLEMA DE CONFIABILIDADE — MEDALHAS PODEM IGNORAR A ELEGIBILIDADE DOS REGISTROS

Arquivo: "app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt"

O que foi identificado

A implementação de medalhas apresenta critérios diferentes entre si. A medalha “Semana de Ferro” utiliza "hasEnoughData", enquanto outras verificam contagens de registros, sequências ou métricas específicas sem necessariamente utilizar o mesmo critério geral de suficiência.

Isso não significa que todas essas medalhas estejam erradas: algumas podem legitimamente depender de dados específicos. O problema é que cada regra precisa garantir, de forma independente, que os registros usados realmente atendam ao requisito descrito.

Por exemplo, a medalha de foco contabiliza registros "DONE" e verifica faltas em hábitos de foco, mas a análise deve confirmar que os registros considerados pertencem à semana, aos dias elegíveis e aos hábitos relevantes. A medalha de hidratação também depende de contagens de conclusão que precisam ser validadas contra o calendário do hábito.

Solução recomendada

1. Definir formalmente o requisito de cada medalha.
2. Filtrar registros por período, hábito, data programada e situação válida antes de calcular o progresso.
3. Separar “progresso calculável” de “medalha conquistada”.
4. Evitar que registros de dias não programados ou dados incompletos produzam conquistas indevidas.
5. Garantir que o texto descritivo corresponda exatamente ao algoritmo.

Testes obrigatórios

- Registros fora da semana.
- Registros em dias nos quais o hábito não estava programado.
- Hábitos criados no meio da semana.
- Registros duplicados ou substituídos.
- Dados insuficientes para cada tipo de medalha.
- Cenários de conquista exatamente no limite do requisito.

Critério de aceitação: nenhuma medalha deve ser concedida sem que todos os seus requisitos estejam comprovados pelos dados elegíveis.

---

4. SEGURANÇA — PARÂMETROS DO PBKDF2 PRECISAM DE REVISÃO

Arquivo: "app/src/main/java/com/example/util/SecurityUtils.kt"

O que foi identificado

O aplicativo já utiliza "PBKDF2WithHmacSHA256", salt aleatório e comparação de hashes por "MessageDigest.isEqual". Esses são avanços em relação ao armazenamento direto de PINs ou ao uso isolado de SHA-256.

Contudo, o código utiliza atualmente 10.000 iterações de PBKDF2. Esse custo é baixo em comparação com referências contemporâneas de armazenamento de senhas, especialmente porque um PIN numérico de quatro dígitos tem um espaço de busca muito pequeno.

O número de iterações, isoladamente, não determina toda a segurança: o custo precisa ser compatível com o desempenho dos dispositivos suportados e com uma política de segurança documentada.

Solução recomendada

1. Definir um parâmetro de derivação mais resistente, orientado por recomendações atuais e por medições em dispositivos Android reais.
2. Armazenar o algoritmo e os parâmetros utilizados junto ao hash, para permitir futuras atualizações.
3. Verificar se o número de iterações lido do banco é válido e está dentro de limites seguros, evitando valores malformados ou excessivos.
4. Considerar a migração gradual de hashes antigos após autenticação válida.
5. Garantir que o PIN em texto simples seja removido assim que a migração for concluída.
6. Documentar os limites de proteção: um PIN local não equivale a uma autenticação forte de conta nem protege contra todos os ataques em um dispositivo comprometido.

Testes obrigatórios

- PIN correto e incorreto.
- Salt diferente para novos PINs.
- Hash legado SHA-256.
- PIN legado em texto simples.
- Hash com formato inválido.
- Número de iterações ausente, inválido ou fora do limite permitido.
- Migração bem-sucedida sem perda das configurações.

Critério de aceitação: as credenciais devem ser verificadas com parâmetros válidos, atualizáveis e documentados, sem persistir o PIN em texto simples após a migração.

---

5. SEGURANÇA — MIGRAÇÃO DE CREDENCIAIS ANTIGAS DEVE SER MAIS ROBUSTA

Arquivos:

- "app/src/main/java/com/example/data/model/UserSettings.kt"
- "app/src/main/java/com/example/data/db/AppDatabase.kt"
- "app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt"
- "app/src/main/java/com/example/util/SecurityUtils.kt"

O que foi identificado

A entidade "UserSettings" ainda possui a propriedade "pinCode", além de "pinHash" e "pinSalt". O código mantém caminhos de compatibilidade com PIN em texto simples e hash legado.

A migração de banco 5→6 adiciona as colunas "pinHash" e "pinSalt", mas não remove a coluna antiga. A presença da coluna, por si só, não é uma vulnerabilidade comprovada; o risco depende dos valores armazenados e de como o processo de migração é executado.

O "MainViewModel" contém lógica para atualizar credenciais antigas, mas o processo deve ser validado para diferentes estados de banco e configuração.

Solução recomendada

1. Documentar a estratégia de migração de cada formato legado.
2. Garantir que uma atualização não deixe o usuário bloqueado nem desative inadvertidamente a proteção.
3. Confirmar que o valor em texto simples é apagado após a migração.
4. Verificar o que acontece se a migração for interrompida ou executada novamente.
5. Planejar a remoção da coluna antiga por meio de uma migração Room apropriada, caso a compatibilidade necessária permita.
6. Preservar os demais dados e configurações do usuário durante todo o processo.

Testes obrigatórios

- Instalação nova.
- Banco antigo com PIN em texto simples.
- Banco com hash SHA-256.
- Banco com PBKDF2.
- Banco com autenticação desativada.
- Interrupção e repetição do processo de atualização.
- Atualização de uma versão antiga até a versão atual.

Critério de aceitação: todas as versões suportadas devem migrar sem perda de dados, sem expor o PIN e sem permitir acesso indevido.

---

6. AUTENTICAÇÃO — TESTAR O CICLO COMPLETO DE BLOQUEIO E DESBLOQUEIO

Arquivos:

- "app/src/main/java/com/example/MainActivity.kt"
- "app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt"
- "app/src/main/java/com/example/ui/components/AuthPinLockScreen.kt"
- "app/src/main/java/com/example/util/SecurityUtils.kt"

Situação atual

O aplicativo já possui um fluxo "AuthState" com os estados "Loading", "Locked" e "Unlocked". Isso é importante porque permite distinguir o carregamento inicial das configurações de uma sessão autenticada.

O bloqueio por tentativas também utiliza preferências persistentes, e a tela consulta o tempo restante do bloqueio.

Portanto, não é correto afirmar que o aplicativo continua necessariamente apresentando o antigo problema de desbloqueio inicial. A implementação atual melhorou esse fluxo, mas ele ainda precisa de testes de regressão.

Riscos que devem ser verificados

- O conteúdo principal pode aparecer antes da confirmação do estado de autenticação?
- O bloqueio continua funcionando depois de minimizar e reabrir o aplicativo?
- A sessão é bloqueada após o tempo configurado?
- O bloqueio permanece depois de reiniciar o processo ou o dispositivo?
- O bloqueio temporário pode ser contornado alterando o relógio do sistema?
- A tela segura contra capturas de tela é aplicada e removida corretamente?

Solução recomendada

1. Tratar "Loading" como um estado que não permite acesso ao conteúdo protegido.
2. Centralizar a política de bloqueio e desbloqueio.
3. Não depender apenas de variáveis temporárias da interface para decisões de segurança.
4. Testar o comportamento após rotação, recriação da atividade, encerramento do processo e reinicialização.
5. Verificar se a tela de bloqueio não permite navegar para telas protegidas por rotas alternativas ou intents.

Critério de aceitação: o conteúdo protegido só deve ser acessível quando o estado de autenticação efetivamente permitir o acesso.

---

7. RELATÓRIOS — VALIDAR OS CÁLCULOS COM CASOS-LIMITE

Arquivo: "app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt"

Melhorias já presentes

A implementação atual de "computeWeeklyReport()" filtra os dias da semana, considera as datas elegíveis de cada hábito e exclui do numerador as conclusões registradas em dias não programados. Há também testes específicos para hábitos criados no meio da semana e hábitos sem dias programados.

Essas melhorias devem ser preservadas.

Pontos que ainda exigem validação

1. Confirmar que todos os indicadores utilizam a mesma janela temporal.
2. Garantir que registros de um hábito excluído ou inativo não alterem indevidamente o resultado.
3. Verificar a relação entre dias com registros, dias programados e número de verificações elegíveis.
4. Confirmar se registros de sono e registros de hábitos devem contribuir da mesma forma para o indicador geral de dados suficientes.
5. Verificar se os valores de adesão são arredondados de forma consistente.
6. Conferir a interpretação dos dias da semana, inclusive em mudanças de fuso horário e virada de data.
7. Revisar o cálculo do desvio-padrão do horário de dormir, especialmente em registros próximos da meia-noite.

Solução recomendada

Criar uma camada de cálculo independente da interface, com funções puras e regras documentadas. A mesma lógica deve alimentar o relatório na tela, as exportações CSV/JSON e as medalhas.

Critério de aceitação: o mesmo conjunto de dados deve gerar resultados equivalentes em todas as formas de apresentação e exportação.

---

8. LEMBRETES E ALARMES — VALIDAR ENTREGA EM SEGUNDO PLANO

Arquivos principais:

- "app/src/main/java/com/example/receiver/AlarmReceiver.kt"
- "app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt"
- "app/src/main/AndroidManifest.xml"

Situação atual

O manifesto declara permissões relacionadas a notificações, alarmes exatos, vibração e inicialização do dispositivo. O projeto também contém um receptor para eventos de alarmes.

Isso demonstra que existe uma estrutura específica para lembretes. Entretanto, a presença das permissões e classes não comprova, sozinha, que todos os alarmes serão entregues nos horários esperados.

Solução recomendada

1. Testar a entrega com o aplicativo aberto, em segundo plano, encerrado e após reinicialização.
2. Verificar reagendamento depois de mudanças de horário, fuso e data.
3. Confirmar o comportamento quando a permissão para alarmes exatos não estiver concedida.
4. Verificar a permissão de notificações no Android 13 ou superior.
5. Garantir que alarmes cancelados ou alterados não continuem disparando.
6. Verificar duplicação de alarmes depois de múltiplas inicializações.
7. Documentar o comportamento esperado em dispositivos que aplicam restrições agressivas de bateria.

Critério de aceitação: cada lembrete deve ter regras previsíveis de criação, cancelamento, reagendamento e entrega, com mensagens claras quando uma permissão necessária estiver ausente.

---

9. WIDGET — PRESERVAR O COMPORTAMENTO ASSÍNCRONO E TESTAR A ATUALIZAÇÃO

Arquivo: "app/src/main/java/com/example/widget/RotinaUpWidgetProvider.kt"

Situação atual

O widget utiliza "goAsync()" nos caminhos de atualização observados, executa operações em uma coroutine no dispatcher de IO e chama "finish()" no bloco "finally". Isso é uma melhoria em relação à execução assíncrona sem controle do ciclo de vida do broadcast.

Riscos a verificar

- O tempo gasto na leitura do banco e na atualização do widget permanece dentro dos limites operacionais.
- Exceções não deixam o resultado visual em um estado inconsistente.
- O widget é atualizado depois de concluir ou alterar um hábito.
- O conteúdo não revela informações que deveriam permanecer ocultas quando o aplicativo está bloqueado.
- A atualização funciona quando o processo do aplicativo é iniciado apenas para atender ao widget.

Solução recomendada

1. Criar testes para atualização com banco vazio, banco populado e falha de leitura.
2. Evitar operações de banco excessivamente demoradas durante o broadcast.
3. Verificar o tratamento de exceções e o encerramento de todos os caminhos com "finish()".
4. Testar a sincronização depois de alterações feitas em outras telas.
5. Verificar o conteúdo do widget com a autenticação ativada.

Critério de aceitação: o widget deve refletir os dados persistidos sem travar, exibir informações indevidas ou perder atualizações silenciosamente.

---

10. BANCO DE DADOS — TESTAR TODAS AS MIGRAÇÕES E A PRESERVAÇÃO DOS DADOS

Arquivo: "app/src/main/java/com/example/data/db/AppDatabase.kt"

Situação atual

O banco está na versão 7, utiliza migrações explícitas e declara "exportSchema = true". Há também um arquivo de esquema versionado em "app/schemas".

Essas são práticas positivas que devem continuar sendo utilizadas.

Riscos que ainda precisam ser eliminados

A existência das migrações não prova que todos os bancos históricos foram testados. Uma alteração pode funcionar em uma instalação limpa e falhar quando o usuário atualiza uma instalação antiga que contém registros reais.

Solução recomendada

1. Criar testes de migração de cada versão suportada até a versão atual.
2. Validar o esquema antes e depois de cada migração.
3. Confirmar a preservação de hábitos, registros, sono, avaliações diárias, eventos de alarme e configurações.
4. Testar dados nulos e valores antigos que possam ter sido produzidos por versões anteriores.
5. Verificar a consistência dos arquivos de esquema versionados.
6. Não recorrer à destruição automática do banco para contornar erros de migração.

Critério de aceitação: a atualização deve preservar todos os dados compatíveis e falhar de maneira explícita e segura se encontrar uma situação não suportada.

---

11. BACKUP — DEFINIR EXPLICITAMENTE O MODELO DE PRIVACIDADE

Arquivos:

- "app/src/main/AndroidManifest.xml"
- "app/src/main/res/xml/backup_rules.xml"
- "app/src/main/res/xml/data_extraction_rules.xml"

O que foi identificado

O aplicativo declara "android:allowBackup="true"" e inclui arquivos do banco de dados nas regras de backup e transferência entre dispositivos. As preferências utilizadas para o bloqueio por tentativas são excluídas dessas regras.

Isso não é automaticamente um defeito: o backup pode ser desejável para preservar as rotinas do usuário. Contudo, o banco contém configurações e dados potencialmente pessoais, e a proteção por PIN deve ser entendida como uma camada local, não como garantia de criptografia do backup.

Solução recomendada

1. Definir se os dados pessoais devem ser copiados para a nuvem e transferidos entre dispositivos.
2. Avaliar se a proteção por PIN precisa ser preservada após uma restauração.
3. Documentar quais dados são incluídos e excluídos.
4. Confirmar o comportamento em restauração, reinstalação e troca de dispositivo.
5. Verificar se as regras são compatíveis com as versões de Android suportadas.
6. Se o modelo de privacidade exigir que os dados nunca sejam incluídos no backup, desativar ou restringir essa funcionalidade deliberadamente.

Critério de aceitação: o comportamento do backup deve corresponder a uma decisão explícita de privacidade, documentada e testada.

---

12. TESTES AUTOMATIZADOS — AMPLIAR A COBERTURA DOS FLUXOS CRÍTICOS

Arquivos relevantes:

- "app/src/test/java/com/example/WeeklyReportTest.kt"
- "app/src/test/java/com/example/SecurityUtilsTest.kt"
- "app/src/test/java/com/example/DatabaseOperationsTest.kt"
- "app/src/test/java/com/example/DateUtilsTest.kt"
- "app/src/test/java/com/example/ExampleRobolectricTest.kt"
- "app/src/test/java/com/example/ExampleUnitTest.kt"

Situação atual

O repositório já possui testes de segurança, relatórios, datas e operações de banco. A auditoria não deve desconsiderar essa cobertura existente.

Lacunas prioritárias

- Migração de todas as versões do banco suportadas.
- Inicialização da autenticação com dados persistidos.
- Bloqueio e desbloqueio depois de recriação da atividade.
- Persistência e expiração de bloqueios temporários.
- Reagendamento e cancelamento de alarmes.
- Atualização do widget.
- Importação e exportação de dados, se aplicável ao fluxo atual.
- Relatórios e medalhas com registros incompletos, inválidos ou fora do período.

Solução recomendada

Separar os testes em três grupos:

1. Testes unitários: regras de negócio, cálculo de adesão, datas, medalhas e validação de credenciais.
2. Testes de integração: banco de dados, migrações, repositórios e sincronização do widget.
3. Testes instrumentados: navegação, autenticação, notificações, permissões e comportamento no Android real.

Critério de aceitação: cada defeito corrigido deve possuir um teste de regressão reproduzível.

---

13. INTEGRAÇÃO CONTÍNUA — TORNAR O RESULTADO DO CI UM CRITÉRIO DE LIBERAÇÃO

Arquivo: ".github/workflows/android.yml"

Situação atual

O repositório possui um workflow que executa os testes unitários com Gradle e constrói o APK de depuração. Isso é positivo.

Melhorias recomendadas

1. Confirmar que o workflow está sendo executado com sucesso na branch principal.
2. Verificar se a versão do JDK utilizada é compatível com a configuração Gradle e Android do projeto.
3. Acrescentar testes de migração e outras verificações automatizadas críticas.
4. Considerar verificações estáticas de Kotlin e análise de dependências.
5. Publicar relatórios de testes como artefatos quando houver falhas.
6. Evitar que uma alteração seja considerada pronta quando os testes ou a compilação falharem.

Critério de aceitação: toda alteração relevante deve passar pelos testes automatizados e pela compilação antes de ser integrada.

---

14. PLANO DE CORREÇÃO PRIORIZADO

Fase 1 — Correções de lógica e segurança

1. Unificar o cálculo de "hasEnoughData" no relatório.
2. Auditar as regras de elegibilidade de cada medalha.
3. Revisar os parâmetros do PBKDF2 e a validação de seus metadados.
4. Confirmar a migração de credenciais legadas.
5. Executar os testes de segurança e acrescentar casos-limite.

Fase 2 — Integridade e confiabilidade

1. Testar todas as migrações Room suportadas.
2. Validar relatórios, exportações e cálculos em cenários extremos.
3. Testar lembretes em segundo plano e depois de reinicializações.
4. Testar o widget com banco vazio, dados existentes e autenticação ativada.
5. Formalizar a política de backup e privacidade.

Fase 3 — Qualidade de entrega

1. Executar a suíte de testes e a compilação.
2. Corrigir falhas encontradas no CI.
3. Acrescentar testes de regressão para cada correção.
4. Executar testes instrumentados nos dispositivos Android suportados.
5. Registrar os resultados em um relatório de validação.

---

15. MATRIZ DE TESTES DE ACEITAÇÃO

Área| Teste mínimo| Resultado esperado
Autenticação| Abrir o app com PIN configurado| Conteúdo protegido só aparece após autenticação
Bloqueio| Repetir PIN incorreto| Bloqueio aplicado e persistido conforme a política
Credenciais| Migrar PIN legado| Hash atualizado e PIN em texto simples apagado
Relatório| Menos de cinco dias registrados| Mensagem de dados insuficientes, sem conclusões indevidas
Relatório| Hábito criado no meio da semana| Dias anteriores à criação não reduzem a adesão
Medalhas| Registros fora do período ou em dias não programados| Não contam para requisitos que não atendem
Banco| Atualizar de versão antiga| Dados preservados e esquema válido
Alarmes| Reiniciar o dispositivo| Alarmes necessários reagendados conforme a política
Notificações| Revogar permissão| Aplicativo informa a limitação sem falha inesperada
Widget| Atualizar após alterar um hábito| Dados exibidos correspondem ao estado persistido
Backup| Restaurar dados| Comportamento conforme a política de privacidade
CI| Executar testes e compilação| Workflow concluído com sucesso

---

16. CONCLUSÃO

O RotinaUp já apresenta uma base de desenvolvimento organizada e várias melhorias importantes. Não seria correto classificá-lo como um projeto sem testes ou sem proteção por PIN: essas estruturas existem e precisam ser aproveitadas.

As prioridades mais claras identificadas nesta análise são:

1. Corrigir a divergência entre a suficiência de dados calculada e a devolvida pelo relatório semanal.
2. Revisar a validação das medalhas para que os resultados sejam sustentados por registros elegíveis.
3. Reavaliar o custo do PBKDF2 e completar a validação das migrações de credenciais.
4. Executar testes de regressão para autenticação, banco, alarmes e widget.
5. Definir e validar a política de backup e privacidade.

As correções devem ser implementadas em pequenas alterações, cada uma acompanhada de testes. Não se deve declarar um problema resolvido apenas porque o código foi alterado: a conclusão deve depender da compilação, dos testes automatizados e, para funcionalidades dependentes do sistema operacional, de validação no Android.

Limitações desta auditoria

- O código foi inspecionado estaticamente.
- Não foi comprovado que todos os testes do repositório passam.
- Não foi realizada validação em dispositivo físico.
- Os riscos descritos como pendentes devem ser confirmados por testes antes de serem classificados como defeitos reproduzidos.
- Este relatório não representa uma certificação formal de segurança.

Resultado final: o projeto tem uma base aproveitável, mas ainda requer correções focadas em consistência dos relatórios, segurança das credenciais e validação dos fluxos críticos antes de se considerar a qualidade integralmente comprovada.