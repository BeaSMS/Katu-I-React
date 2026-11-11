# Especificação de Requisitos — Katu'I

## Escopos do Site e do Servidor

| Informação | Descrição |
|---|---|
| **Sistema** | Katu'I — aplicação de organização de informações de saúde |
| **Componentes cobertos** | Site Web (frontend) e Servidor (API e banco de dados) |
| **Finalidade** | Especificar o escopo mínimo, médio e opcional do Site e do Servidor |

---

## 1. Introdução

### 1.1 Objetivo

Este documento especifica os requisitos do sistema básico Katu'I. A aplicação permite registrar e consultar informações de saúde da própria pessoa usuária e, opcionalmente, de pessoas sob seus cuidados.

Os requisitos estão organizados por nível de escopo e componente:

- **Escopo mínimo:** funcionalidades indispensáveis para uma primeira versão utilizável, segura e coerente.
- **Escopo médio:** módulos e melhorias importantes para ampliar a utilidade do sistema após a entrega mínima.
- **Escopo opcional:** extensões, automações e integrações que podem ser adicionadas conforme prioridade, viabilidade e dependências.
- **Site:** interface Web, formulários, navegação e experiência da pessoa usuária. No modo Guest, também mantém dados localmente.
- **Servidor:** API e persistência remota, responsáveis por autenticar, autorizar, validar e sincronizar dados de contas.

O Site pode ser usado no modo Guest sem o Servidor. Para criar uma conta, entrar e sincronizar dados entre sessões ou dispositivos, o Site depende do Servidor.

Os requisitos não funcionais de privacidade, segurança, integridade e comunicação responsável aplicam-se a todos os níveis. Requisitos de níveis posteriores não substituem nem reduzem os controles definidos para o escopo mínimo.

### 1.2 Limites do produto

O Katu'I é uma ferramenta de organização pessoal. Não substitui atendimento profissional, não realiza diagnóstico clínico e não deve apresentar previsões ou sugestões automatizadas como prescrição ou orientação médica.

Este documento cobre o Site Web e o Servidor. Aplicativos para outras plataformas não fazem parte deste escopo.

### 1.3 Termos

| Termo | Significado |
|---|---|
| **Pessoa usuária** | Pessoa que acessa o Katu'I por meio do Site. |
| **Perfil de cuidado** | Conjunto de informações associado à própria pessoa ou, opcionalmente, a um dependente. |
| **Perfil ativo** | Perfil selecionado no Site e usado como contexto para exibir ou registrar dados. |
| **Guest** | Modo de uso sem conta, com dados mantidos localmente no dispositivo/navegador. |
| **Registro** | Informação de saúde cadastrada, como medicamento, consulta, sintoma ou medição. |
| **Push** | Serviço externo de notificações que pode enviar lembretes mesmo quando o Site não está aberto, sujeito a configuração e permissões. |

---

## 2. Visão geral do sistema

### 2.1 Componentes e responsabilidades

#### Site Web

O Site deve:

- apresentar as telas, menus, formulários e listas dos módulos;
- permitir uso local em modo Guest;
- enviar ao Servidor as solicitações de conta e de dados sincronizados;
- indicar o perfil e a data selecionados;
- validar campos para orientar o preenchimento, sem substituir a validação do Servidor;
- informar carregamento, sucesso, falhas de rede, erros de validação e falta de autorização;
- proteger a sessão local e não incluir credenciais no backup exportado.

#### Servidor

O Servidor deve:

- disponibilizar a API para cadastro, autenticação e operações sobre dados;
- persistir dados associados à conta e ao perfil de cuidado;
- validar novamente os dados recebidos pelo Site;
- autorizar cada operação considerando a conta autenticada e a propriedade do perfil;
- devolver respostas e erros em formato consistente;
- disponibilizar dados para consulta e sincronização quando houver conexão.

### 2.2 Fluxo principal

1. A pessoa abre o Site e escolhe continuar como Guest ou criar uma conta/entrar.
2. No modo autenticado, o Site solicita ao Servidor a sessão e os perfis disponíveis.
3. A pessoa escolhe ou configura o perfil de cuidado ativo.
4. A pessoa registra e consulta informações nos módulos disponíveis.
5. Em Guest, os dados são mantidos localmente. Com conta, o Site consulta e atualiza os dados no Servidor.
6. Se ocorrer falha de rede, o Site informa o estado e pode manter em fila apenas operações compatíveis com a sincronização offline.
7. O Servidor verifica a autorização e a validade dos dados antes de persistir qualquer operação autenticada.

---

## 3. Requisitos funcionais — escopo mínimo

Os itens a seguir compõem a funcionalidade mínima prevista para o Site e o Servidor.

### RFN-01 — Acesso ao sistema `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve permitir o uso local sem conta e o acesso autenticado para quem deseja sincronizar seus dados.

**Responsabilidade do Site**

- Apresentar as opções de continuar como Guest, criar conta e entrar.
- No cadastro, solicitar nome, e-mail, senha e confirmação de senha.
- No login, solicitar e-mail e senha.
- Validar campos obrigatórios e apresentar mensagens compreensíveis quando o formulário estiver incorreto.
- No modo Guest, permitir entrar nos recursos locais sem solicitar credenciais.
- Em uso autenticado, manter a sessão conforme o mecanismo definido para o sistema e solicitar novo acesso quando ela expirar ou for recusada.

**Responsabilidade do Servidor**

- Criar uma conta a partir dos dados válidos recebidos.
- Autenticar as credenciais de login e devolver uma sessão/token utilizável pela API.
- Recusar credenciais inválidas ou solicitações malformadas com erro apropriado.
- Associar os perfis e registros autenticados à conta correspondente.

**Critérios de aceitação**

1. É possível abrir o modo Guest sem conexão com o Servidor.
2. Uma conta válida pode ser criada e utilizada para entrar.
3. Credenciais inválidas não iniciam uma sessão.
4. Uma falha no cadastro ou login é apresentada como falha, sem indicar sucesso.

**Escopo:** mínimo.

### RFN-02 — Perfil principal de cuidado `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve oferecer um perfil principal para organizar os registros de saúde.

**Responsabilidade do Site**

- Apresentar o fluxo inicial para configurar o perfil principal.
- Permitir informar os dados básicos de perfil solicitados pelo formulário.
- Indicar qual perfil está ativo durante a navegação.
- Exibir registros dentro do contexto do perfil selecionado.

**Responsabilidade do Servidor**

- Criar e devolver um identificador estável para o perfil principal autenticado.
- Manter a relação entre a conta proprietária e o perfil.
- Associar os registros enviados ao perfil autorizado.
- Recusar operações que tentem acessar perfil de outra conta.

**Critérios de aceitação**

1. Uma pessoa pode concluir a configuração do perfil principal.
2. Os registros criados ficam associados ao perfil correto.
3. O Site informa qual perfil está sendo usado.
4. O Servidor não aceita como autorização apenas um identificador de perfil enviado pelo cliente.

**Escopo:** mínimo.

### RFN-03 — Registro e consulta de medicamentos `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve permitir organizar medicamentos e tratamentos em uso.

**Responsabilidade do Site**

- Apresentar formulário para cadastrar um medicamento.
- Solicitar, no mínimo, nome, dose/descrição, finalidade, data de início, horário e esquema de uso conforme o formulário.
- Permitir informar dados complementares disponíveis, como frequência, duração, uso contínuo, quantidade, unidade e estoque mínimo.
- Exibir a lista de medicamentos e permitir consultar os detalhes disponíveis.
- Apresentar o estado de salvamento ou o erro retornado.

**Responsabilidade do Servidor**

- Validar os dados e associar o medicamento à conta e ao perfil autorizados.
- Persistir o registro e devolver os dados salvos com identificador estável.
- Disponibilizar operações de listagem, edição e remoção.
- Preservar datas e duração para que o Site possa determinar em quais dias o tratamento se aplica.

**Critérios de aceitação**

1. Um medicamento com os campos obrigatórios pode ser cadastrado.
2. O registro salvo aparece na lista do perfil correto.
3. Dados opcionais não informados não impedem o cadastro quando o formulário os define como opcionais.
4. Uma falha de validação ou persistência é comunicada e não aparece como salvamento concluído.

**Escopo:** mínimo.

### RFN-04 — Registro e consulta de consultas de saúde `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve permitir organizar compromissos de saúde.

**Responsabilidade do Site**

- Solicitar o nome do profissional ou referência da consulta.
- Disponibilizar campos para especialidade, local/clínica, data, horário e observações.
- Listar consultas do perfil e permitir localizar a consulta pela data.

**Responsabilidade do Servidor**

- Validar e persistir a consulta associada à conta e ao perfil.
- Devolver a consulta criada com identificador estável.
- Disponibilizar operações de consulta, edição e remoção.

**Critérios de aceitação**

1. A consulta exige ao menos a identificação do profissional/referência e a data/horário previstos pelo formulário.
2. Depois do salvamento, a consulta aparece no histórico do perfil e na data correspondente.
3. Uma pessoa não consegue consultar ou alterar consultas pertencentes a outra conta.

**Escopo:** mínimo.

### RFN-05 — Registro e consulta de exames `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve permitir manter um histórico básico de exames.

**Responsabilidade do Site**

- Solicitar o nome do exame.
- Disponibilizar categoria, tipo de resultado, data e observações.
- Listar exames do perfil e permitir localizá-los por data.
- Não apresentar anexação de laudo como disponível neste fluxo; o cadastro de arquivos está fora do escopo básico.

**Responsabilidade do Servidor**

- Validar e persistir exames associados à conta e ao perfil.
- Devolver os registros para consulta no histórico.
- Oferecer operações de edição e remoção.

**Critérios de aceitação**

1. Um exame pode ser cadastrado com nome e data.
2. Categoria, tipo de resultado e observações são armazenados quando informados.
3. O exame aparece no perfil correto e pode ser encontrado pela data.
4. O Site não oferece anexação de laudo no cadastro de exames.

**Escopo:** mínimo.

### RFN-06 — Diário de sintomas e bem-estar `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve permitir registrar sintomas físicos e informações de humor/bem-estar.

**Responsabilidade do Site**

- Permitir escolher entre registro físico e registro de humor.
- Para registro físico, solicitar descrição do sintoma e oferecer campos complementares como localização, duração, fatores, impacto e intensidade.
- Para registro de humor, solicitar o estado/clima e permitir registrar nuances e intensidade quando disponíveis.
- Associar o registro a uma data e apresentar o histórico do perfil.
- Não interpretar o registro como diagnóstico.

**Responsabilidade do Servidor**

- Validar o tipo de registro e seus campos obrigatórios.
- Persistir e devolver o registro associado à conta e ao perfil autorizados.
- Preservar a data, o tipo e os campos específicos do registro.

**Critérios de aceitação**

1. Um registro físico sem descrição é recusado pelo formulário e também validado pelo Servidor.
2. Um registro de humor exige a seleção de um estado/clima.
3. Registros salvos aparecem no histórico do perfil e podem ser filtrados por data.
4. O sistema não apresenta conclusões clínicas com base nos dados registrados.

**Escopo:** mínimo.

### RFN-07 — Registro e consulta de sinais vitais `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve permitir registrar medições dos indicadores disponíveis.

**Indicadores previstos:** pressão arterial, glicose, batimentos cardíacos, saturação, temperatura, peso e altura.

**Responsabilidade do Site**

- Permitir selecionar o tipo de medição.
- Solicitar os valores e unidades adequados ao tipo escolhido.
- Solicitar data/hora da medição e permitir observações quando disponíveis.
- Exibir medições do perfil e filtrar os registros por data.

**Responsabilidade do Servidor**

- Validar que os campos enviados correspondem ao tipo da medição.
- Para pressão arterial, validar o recebimento dos valores sistólico e diastólico.
- Persistir e devolver cada medição associada à conta e ao perfil.

**Critérios de aceitação**

1. A medição só pode ser salva com o valor correspondente ao tipo escolhido.
2. A pressão arterial exige os valores sistólico e diastólico.
3. Data e horário são preservados na consulta do histórico.
4. O sistema apresenta os valores registrados, sem emitir diagnóstico automático.

**Escopo:** mínimo.

### RFN-08 — Calendário e consulta por período `[ESCOPO MÍNIMO]`

**Descrição:** o sistema deve facilitar a localização de registros por data.

**Responsabilidade do Site**

- Exibir calendário ou controle equivalente para selecionar uma data.
- Filtrar listas e resumos usando a data selecionada.
- Considerar intervalos de tratamento quando aplicável, em vez de tratar todo medicamento como registro de um único dia.
- Distinguir ausência de registros de falha no carregamento.

**Responsabilidade do Servidor**

- Retornar datas e intervalos em formato consistente para os registros.
- Restringir os dados devolvidos ao perfil autorizado.
- Disponibilizar os registros necessários às telas de histórico e resumo.

**Critérios de aceitação**

1. Ao mudar a data, a lista apresenta apenas os registros aplicáveis à seleção.
2. Registros associados a intervalos são encontrados dentro do período correspondente.
3. Um erro da API não é apresentado como lista vazia bem-sucedida.

**Escopo:** mínimo.

### RFN-09 — Operações sobre registros e mensagens de estado `[ESCOPO MÍNIMO]`

**Descrição:** os módulos básicos devem permitir manter os registros e informar claramente o resultado das operações.

**Responsabilidade do Site**

- Oferecer consulta e cadastro dos registros nos módulos necessários.
- Apresentar remoção onde houver ação correspondente e solicitar confirmação para ações destrutivas quando previsto pela interface.
- Oferecer edição dos registros.
- Apresentar estados de carregamento, lista vazia, sucesso e erro de forma distinta.

**Responsabilidade do Servidor**

- Disponibilizar operações de leitura e gravação necessárias a cada módulo.
- Aplicar validação, autenticação e autorização em todas as operações.
- Devolver status e mensagens que permitam ao Site distinguir sucesso, validação, falta de autorização e indisponibilidade.
- Não devolver sucesso para operação que não foi concluída.

**Critérios de aceitação**

1. Um registro criado pode ser consultado novamente.
2. Uma ação de remoção só é confirmada após sucesso do Servidor ou da persistência local Guest.
3. Falhas são apresentadas como falhas e não como lista vazia ou salvamento concluído.
4. A interface oferece controles de edição e remoção para os registros, com confirmação para remoções e mensagens claras para falhas.

**Escopo:** mínimo.

### RFN-10 — Modo Guest e operação offline básica `[GUEST LOCAL: MÍNIMO; FILA OFFLINE: MÉDIO]`

**Descrição:** o Site deve permitir uso básico sem conexão e preservar operações compatíveis quando houver falha de rede.

**Responsabilidade do Site**

- Manter os dados Guest localmente no dispositivo/navegador.
- Permitir consultar os dados Guest sem conexão com o Servidor.
- No mínimo, informar falhas de conexão durante uso autenticado.
- No escopo médio, indicar operações autenticadas pendentes e enfileirar somente operações que possam ser sincronizadas com segurança após falha de rede/timeout.
- Nunca converter erros de validação, autenticação ou autorização em sucesso offline.

**Responsabilidade do Servidor**

- No mínimo, receber, revalidar e autorizar operações autenticadas quando a conexão estiver disponível.
- No escopo médio, responder explicitamente a conflitos ou operações recusadas durante a sincronização.

**Critérios de aceitação**

1. Os dados Guest permanecem disponíveis após recarregar o Site e sem conexão, dentro dos limites do armazenamento local.
2. No escopo médio, o Site indica quando dados autenticados aguardam sincronização.
3. No escopo médio, dados sincronizados só são considerados concluídos após confirmação do Servidor.
4. No escopo médio, erros de validação/autorização nunca entram na fila como gravações bem-sucedidas.

**Escopo:** Guest local no mínimo; fila de operações autenticadas e resolução de conflitos no escopo médio.

### RFN-11 — Segurança, autorização e isolamento de dados `[ESCOPO MÍNIMO — TRANSVERSAL]`

**Descrição:** dados de saúde devem ser acessíveis somente à conta e ao perfil autorizados.

**Responsabilidade do Site**

- Enviar credenciais de sessão nos pedidos autenticados.
- Manter o perfil selecionado como contexto das telas e solicitações.
- Não expor credenciais ou tokens em telas, logs de usuário ou arquivos de backup.
- Limpar ou invalidar a sessão local quando o acesso for recusado ou encerrado.

**Responsabilidade do Servidor**

- Autenticar cada requisição protegida.
- Verificar se a conta autenticada é proprietária do perfil e dos registros solicitados.
- Aplicar autorização em listagem, criação, edição, remoção, exportação e sincronização.
- Não confiar exclusivamente em um identificador de perfil enviado pelo Site.
- Responder com erro apropriado quando a sessão for inválida ou o recurso não pertencer à conta.

**Critérios de aceitação**

1. Uma pessoa autenticada só acessa os dados de perfis aos quais tem autorização.
2. Alterar manualmente o identificador do perfil na solicitação não permite acessar dados de outra conta.
3. Sessões inválidas não permitem operações protegidas.
4. Backup exportado não contém token de autenticação.

**Escopo:** mínimo e obrigatório em todos os níveis.

---

## 4. Requisitos funcionais — escopos médio e opcional

### RFO-01 — Perfis dependentes `[ESCOPO MÉDIO]`

**Descrição:** permitir que uma pessoa usuária organize dados de mais de um perfil de cuidado.

- **Site:** criar e listar perfis dependentes, exibir a relação informada e permitir alternar o perfil ativo.
- **Servidor:** persistir perfis dentro da conta proprietária e autorizar toda consulta ou alteração pelo vínculo entre conta e perfil.
- **Critério de aceitação:** ao trocar de perfil, o Site apresenta somente os registros daquele perfil.

### RFO-02 — Alarmes e lembretes `[ALARME: MÉDIO; PUSH: OPCIONAL]`

**Descrição:** criar lembretes associados a uma data/horário e, opcionalmente, a um medicamento.

- **Site:** cadastrar título, data, horário, medicamento relacionado e observações; listar próximos lembretes; solicitar permissão de notificações quando necessária.
- **Escopo médio — alarmes:** cadastrar, listar e persistir lembretes associados ao perfil.
- **Escopo opcional — Push:** para notificações em segundo plano, integrar um serviço Push e validar a sessão e o perfil.
- **Critério de aceitação:** a interface distingue alarme cadastrado de notificação efetivamente ativada; indisponibilidade do Push não apaga o registro do lembrete.
- **Dependências:** permissão do navegador/dispositivo, suporte da plataforma e serviço externo configurado.

### RFO-03 — Saúde feminina e acompanhamento de gestação `[ESCOPO MÉDIO]`

**Descrição:** registrar informações de ciclo menstrual, sintomas/fluxo, humor, uso de anticoncepcional e, separadamente, acompanhamento de gestação.

- **Site:** disponibilizar formulários e histórico próprios; permitir registrar início/fim do ciclo, informações diárias e doses de anticoncepcional.
- **Servidor:** persistir os tipos de registro associados ao perfil; validar as datas e relações com medicamentos quando enviados.
- **Critério de aceitação:** registros podem ser consultados por perfil e data; estimativas são identificadas como aproximadas e não como aconselhamento.

### RFO-04 — Carteira de vacinação `[ESCOPO MÉDIO]`

**Descrição:** manter um histórico de vacinas aplicadas.

- **Site:** registrar nome, data conhecida ou desconhecida e observações; listar vacinas do perfil.
- **Servidor:** validar e persistir registros associados à conta e ao perfil.
- **Critério de aceitação:** a pessoa consegue consultar a vacina e distinguir uma data desconhecida de uma data informada.

### RFO-05 — Receitas e leitura assistida `[CADASTRO MANUAL: MÉDIO; ANÁLISE AUTOMATIZADA: OPCIONAL]`

**Descrição:** organizar receitas e, como extensão, solicitar análise automatizada de uma imagem ou PDF.

- **Escopo médio — cadastro manual:** permitir cadastrar e consultar receitas com título/nome, data e observações; persistir os dados associados ao perfil autorizado.
- **Escopo opcional — leitura assistida:** permitir envio do arquivo aceito, apresentar sugestões e avisos para revisão e exigir confirmação explícita antes de criar medicamentos. O Servidor recebe o arquivo somente com integração configurada e devolve sugestões revisáveis; nunca cria medicamentos automaticamente a partir da análise.
- **Critério de aceitação:** nenhuma sugestão automatizada é salva como medicamento antes da confirmação da pessoa usuária; falhas da integração são informadas claramente.
- **Dependências opcionais:** provedor de IA, configuração segura no servidor, limites de formato/tamanho e política de retenção de arquivos.

### RFO-06 — Documentos e anexos `[METADADOS: MÉDIO; ANEXOS: OPCIONAL]`

**Descrição:** organizar documentos de saúde; anexos são uma extensão a ser definida.

- **Escopo médio — metadados:** cadastrar título, detalhes, data e observações e persistir esses dados associados ao perfil autorizado.
- **Escopo opcional — anexos:** permitir envio de arquivos somente após definir armazenamento seguro, formatos e limites. O Servidor deve controlar o acesso e impor os formatos e tamanhos permitidos.
- **Critério de aceitação:** a interface deixa claro se um documento é apenas um registro descritivo ou contém arquivo anexado.

### RFO-07 — Backup pessoal `[BACKUP LOCAL: MÉDIO; SINCRONIZAÇÃO REMOTA: OPCIONAL]`

**Descrição:** permitir exportar/importar dados pessoais e, opcionalmente, sincronizar backup com o Servidor.

- **Escopo médio — backup local:** gerar arquivo de backup, permitir selecionar um arquivo de importação e explicar o resultado antes/depois da operação.
- **Escopo opcional — sincronização remota:** autenticar a solicitação, validar propriedade e persistir somente os dados previstos.
- **Critério de aceitação:** o backup não contém token de sessão; uma importação inválida não substitui silenciosamente os dados atuais.
- **Dependências:** definição de formato, compatibilidade entre versões e política de recuperação.

### RFO-08 — Personalização e acessibilidade ampliada `[ESCOPO MÉDIO]`

**Descrição:** permitir adaptar a apresentação do Site às preferências da pessoa usuária.

- **Site:** oferecer tema, cor do perfil, tamanho de texto, alto contraste, fonte para dislexia e redução de animações.
- **Servidor:** opcionalmente persistir preferências associadas ao perfil para sincronizá-las entre dispositivos.
- **Critério de aceitação:** preferências aplicadas permanecem ativas durante a navegação e, se persistidas, são recuperadas ao entrar em outra sessão.

### RFO-09 — Resumo semanal e notificações Push `[ESCOPO OPCIONAL]`

**Descrição:** complementar os lembretes com resumos e notificações em segundo plano.

- **Site:** permitir ativar/desativar preferências e exibir se o navegador autorizou notificações.
- **Servidor/integração:** agendar e enviar mensagens somente com serviço Push configurado; respeitar preferências, autorização e vínculo com a conta/perfil.
- **Critério de aceitação:** desativar a preferência interrompe novos envios; falha ou ausência do serviço não é apresentada como notificação enviada.
- **Dependências:** serviço externo, configuração de chaves, permissão do navegador e suporte da plataforma.

### RFO-10 — Cadastro estruturado de doenças `[ESCOPO OPCIONAL]`

**Descrição:** criar um módulo específico para doenças, histórico e acompanhamento, se aprovado para o escopo do projeto.

- **Site:** definir campos, histórico, edição e regras de apresentação com responsável pelo produto e revisão adequada.
- **Servidor:** definir modelo, validações, operações e autorização por perfil antes de aceitar dados.
- **Critério de aceitação:** o módulo deve ser distinto de observações livres ou do campo de condições especiais do perfil.

---

## 5. Requisitos não funcionais — mínimos e transversais

### RNF-01 — Privacidade e segurança `[MÍNIMO — TRANSVERSAL]`

- **Site:** proteger a sessão local; evitar expor tokens em telas, logs ou backups; utilizar comunicação segura em produção.
- **Servidor:** proteger credenciais e dados armazenados; autenticar e autorizar operações; separar dados por conta e perfil; não expor segredos em respostas.
- **Verificação:** chamadas sem sessão válida são recusadas; uma conta não acessa dados pertencentes a outra; backups não incluem credenciais de sessão.

### RNF-02 — Integridade e consistência dos dados `[MÍNIMO — TRANSVERSAL]`

- **Site:** validar campos para orientar o preenchimento e informar falhas de gravação sem exibir falso sucesso.
- **Servidor:** repetir validações relevantes, manter associações entre conta, perfil e registro, e garantir formatos de data consistentes.
- **Verificação:** dados inválidos são recusados; registros persistidos podem ser consultados com os mesmos valores e relações essenciais.

### RNF-03 — Usabilidade e compreensão de estados `[MÍNIMO — TRANSVERSAL]`

- **Site:** diferenciar carregamento, lista vazia, desconexão, sucesso e erro; manter visíveis o perfil e a data ativos. Indicar pendência de sincronização quando a fila do escopo médio estiver habilitada.
- **Servidor:** responder com status e estrutura de erro consistentes, permitindo ao Site apresentar mensagens adequadas.
- **Verificação:** uma falha de rede ou API nunca é mostrada como se fosse uma consulta vazia bem-sucedida.

### RNF-04 — Acessibilidade `[MÍNIMO — TRANSVERSAL]`

- **Mínimo:** manter navegação e controles perceptíveis e utilizáveis, com identificação acessível e contraste suficiente.
- **Médio:** oferecer opções de ampliação de texto, alto contraste e redução de animações.
- **Servidor:** fornecer mensagens e dados em formato que permita apresentação acessível no Site.
- **Verificação:** controles têm identificação e retorno visual compreensíveis; conteúdo permanece utilizável com as configurações de acessibilidade habilitadas.

### RNF-05 — Compatibilidade entre Site e API `[MÍNIMO — TRANSVERSAL]`

- **Site:** enviar dados conforme o contrato da API e tratar respostas, sessão expirada e indisponibilidade.
- **Servidor:** manter rotas, métodos, campos e formatos de resposta documentados para as funcionalidades ativadas.
- **Verificação:** os fluxos de cadastro, consulta e gravação seguem o contrato publicado; mudanças incompatíveis são documentadas e tratadas pelo cliente.

### RNF-06 — Limites clínicos e comunicação responsável `[MÍNIMO — TRANSVERSAL]`

- **Site:** deixar claro que registros, previsões de ciclo e sugestões automatizadas não são diagnóstico ou recomendação médica.
- **Servidor/integrações:** não tratar conteúdo de IA como prescrição confirmada; retornar avisos e falhas de integração de forma explícita.
- **Verificação:** conteúdo automatizado não cria tratamentos sem revisão e confirmação da pessoa usuária.

---

## 6. Regras gerais de dados e operações `[ESCOPO MÍNIMO — TRANSVERSAL]`

1. Todo registro autenticado deve pertencer a uma conta e a um perfil autorizado.
2. A identificação do perfil enviada pelo Site serve para selecionar o contexto, mas não substitui a autorização feita pelo Servidor.
3. O Site deve distinguir erro de carregamento de uma lista sem registros.
4. O Site deve distinguir operação local Guest de operação persistida no Servidor.
5. Se a fila offline do escopo médio estiver habilitada, ela deve receber somente operações compatíveis com falhas de rede/timeout; erros HTTP de validação ou autorização devem continuar visíveis.
6. Datas sem horário devem seguir o formato adotado pelo contrato da API (`YYYY-MM-DD`); horários devem seguir o formato correspondente (`HH:mm`) quando aplicável.
7. Anexos, notificações Push, IA e sincronização avançada só fazem parte do escopo quando suas dependências e limites estiverem definidos.

---

## 7. Limites de escopo e itens condicionais

Os itens a seguir não fazem parte do escopo mínimo. Quando houver requisito médio ou opcional correspondente, aplicam-se as classificações da seção 4. Integrações e arquivos só devem ser habilitados após definir dependências, limites e requisitos de segurança.

- **Fora do escopo do produto:** diagnóstico, prescrição ou recomendação clínica automatizada.
- **Fora dos escopos mínimo e médio; opcional mediante aprovação:** upload de laudos no formulário de exames.
- **Opcional:** upload de arquivos no formulário genérico de documentos, conforme RFO-06.
- **Opcional e condicionado:** leitura de receita por IA, somente com provedor externo configurado.
- **Opcional e condicionado:** notificações em segundo plano, somente com serviço Push e permissão da plataforma; não há garantia de entrega.
- **Opcional:** cadastro estruturado de doenças, conforme RFO-10.
- **Opcional e condicionado:** sincronização remota dos dados Guest, somente com fluxo de conta, autorização e consentimento definido.

---

## 8. Especificação detalhada do Frontend por escopo

Esta seção especifica o comportamento e os elementos visuais que devem ser construídos no Site.

### 8.1 Estrutura comum do Site autenticado

#### FE-01 — Navegação principal `[MÍNIMO; MÓDULOS MÉDIOS/OPCIONAIS CONDICIONAIS]`

- O menu deve permitir acessar Visão geral, Medicamentos, Sintomas, Sinais vitais, Consultas, Exames, Ajuda e Configurações no escopo mínimo. Exibir Alarmes, Receitas, Documentos, Saúde feminina, Vacinas, Perfis e demais módulos conforme os respectivos escopos médio/opcional estejam habilitados.
- A barra superior deve identificar a seção e o perfil principal no escopo mínimo; disponibilizar seletor de perfis dependentes no escopo médio.
- O indicador deve diferenciar conectado/desconectado no escopo mínimo e, no médio, sincronização em andamento e operações pendentes.
- O acesso a alarmes deve ser exibido no escopo médio e informar os próximos lembretes; notificações Push são opcionais.
- O controle de sair deve encerrar a sessão local. A exclusão local de dados deve ser uma ação separada, claramente identificada e confirmada.
- Cada item de navegação deve ter nome compreensível e estado visual de seleção; no celular, o menu deve permanecer acessível sem encobrir o conteúdo principal.

#### FE-02 — Calendário e contexto de data `[ESCOPO MÍNIMO]`

- Exibir mês e ano, controles de mês anterior/próximo e dias selecionáveis.
- Indicar visualmente o dia selecionado e dias com registros dos módulos habilitados; os marcadores devem indicar tipos de atividade sem depender apenas de cor.
- Disponibilizar rótulos acessíveis para os dias e seus marcadores.
- Ao selecionar um dia, atualizar as listas e resumos dependentes da data.
- Fornecer estado de carregamento/erro próprio para a busca de registros do calendário.
- Em telas com muitos dados, permitir recolher ou expandir o calendário sem perder a data selecionada.

#### FE-03 — Padrão das telas de lista `[ESCOPO MÍNIMO]`

Cada tela de coleção deve apresentar:

1. Título da área e uma descrição curta de sua finalidade.
2. Ação principal para adicionar um registro.
3. Calendário/seleção de data quando o recurso for temporal.
4. Lista de registros correspondentes ao perfil e à data ativa.
5. Ação para atualizar a lista.
6. Estado de carregamento, erro, vazio e conteúdo carregado, sem misturá-los.

Cada linha de registro deve mostrar, conforme o tipo:

- nome ou título principal;
- detalhe identificador (dose, especialidade, categoria, data ou descrição);
- data/horário quando relevante;
- estado, quando o recurso possuir um estado;
- ações de abrir, editar e remover, conforme as operações definidas para o tipo de registro.

Para remover, a interface deve solicitar confirmação, indicar qual registro será removido e atualizar a lista somente após a operação concluir. Se falhar, deve preservar o registro e apresentar o erro.

### 8.2 Telas públicas, acesso e configuração inicial

#### FE-04 — Início, cadastro e entrada `[ESCOPO MÍNIMO]`

- A tela inicial deve explicar brevemente a finalidade do Katu'I e oferecer ações claras para entrar, criar conta ou continuar como Guest.
- O formulário de cadastro deve conter nome, e-mail, senha e confirmação de senha, mostrar erros junto aos campos e impedir envio quando os valores obrigatórios forem inválidos.
- **Médio — importação de dados Guest:** se houver dados Guest, apresentar opção explícita de consentir com sua importação para a conta; não importar sem consentimento.
- O formulário de entrada deve solicitar e-mail e senha, indicar erro de autenticação sem revelar informação sensível e permitir nova tentativa.
- Estados de envio devem impedir submissões duplicadas e informar processamento.

#### FE-05 — Integração inicial (onboarding) `[ESCOPO MÍNIMO; PREFERÊNCIAS NO MÉDIO]`

- Apresentar etapas em ordem compreensível: boas-vindas, perfil/dados pessoais e revisão no escopo mínimo; informações iniciais de saúde e preferências no médio.
- Permitir voltar e corrigir campos antes da conclusão.
- Indicar quais campos são obrigatórios e quais são opcionais.
- Para conta autenticada, permitir continuar/pular as partes que a aplicação define como opcionais.
- Em Guest, criar o perfil local necessário antes de acessar o espaço de registros.
- Ao concluir, exibir confirmação e encaminhar à Visão geral.

**Dados do onboarding:** nome e dados básicos do perfil, idade, sexo, peso, altura, alergias, condições especiais, cor do perfil e preferências. O fluxo deve indicar quais dados são obrigatórios ou opcionais em cada etapa e modo de uso.

### 8.3 Visão geral

#### FE-06 — Painel inicial `[ESCOPO MÍNIMO]`

A Visão geral deve mostrar informações do perfil ativo e da data selecionada:

- calendário com marcação de dias com atividade;
- cartão/indicador de quantidade de medicamentos aplicáveis à data;
- cartão/indicador de quantidade de medições de sinais vitais na data;
- indicação da consulta marcada para a data ou mensagem de que não há consulta;
- destaque de próximo medicamento/horário quando houver;
- quantidade de registros de sintomas do dia e atalho para registrar como a pessoa está;
- ações rápidas para criar registros nos módulos principais;
- atalhos para abrir as listas detalhadas.

Cada cartão clicável deve levar à tela correspondente. O painel deve mostrar carregamento, erro e ausência de dados de forma distinta. Não deve trocar erro da API por valores zero ou por uma mensagem de ausência de registros.

### 8.4 Módulos e detalhes visuais

#### FE-07 — Medicamentos `[ESCOPO MÍNIMO]`

- A lista deve mostrar tratamentos do perfil, respeitando a data selecionada e o intervalo entre início e fim quando houver.
- A linha/cartão deve apresentar nome, dose ou finalidade, horário/frequência e estado, quando disponíveis.
- A área de resumo pode apresentar tratamentos ativos no mínimo; próximo lembrete e indicadores de estoque devem seguir as funcionalidades habilitadas no escopo médio.
- O formulário deve permitir nome (obrigatório), dose, finalidade, início, horário, frequência, duração ou uso contínuo, quantidade, estoque mínimo, dose por tomada e unidade. A geração de alarmes pertence ao escopo médio.
- O botão de adicionar deve abrir o formulário de medicamento; o cadastro de anticoncepcional deve usar os campos e fluxo próprios.
- Quando estoque não foi informado, mostrar “sem informação”/“não cadastrado”, nunca calcular um estado de estoque como se houvesse dados.

#### FE-08 — Alarmes `[ESCOPO MÉDIO; NOTIFICAÇÕES PUSH OPCIONAIS]`

- Mostrar lembretes futuros, com nome, medicamento associado, data, horário e observações disponíveis.
- Permitir criar e remover lembretes com confirmação.
- Distinguir alarme cadastrado de notificação do sistema efetivamente autorizada/entregue.
- Ocultar alarmes concluídos ou apresentá-los em uma seção separada.
- Informar limitações de notificação quando Guest ou o serviço Push não oferecer execução em segundo plano.

#### FE-09 — Receitas `[CADASTRO MANUAL: MÉDIO; LEITURA AUTOMATIZADA: OPCIONAL]`

- Permitir cadastrar manualmente o título/nome, data e observações.
- **Opcional — leitura automatizada:** indicar formatos aceitos e informar que o arquivo será enviado para análise.
- **Opcional — revisão:** durante a análise, bloquear envio repetido e exibir andamento/erro; apresentar cada medicamento sugerido, dose/esquema e avisos; permitir selecionar ou retirar itens.
- **Opcional — confirmação:** exigir confirmação explícita antes de criar medicamentos e apresentar o resultado. Não mostrar a ação de IA como disponível quando o serviço não estiver configurado.

#### FE-10 — Diário de sintomas `[ESCOPO MÍNIMO]`

- Permitir alternar entre registro físico e registro de humor.
- No registro físico, apresentar descrição obrigatória; localização, duração, fatores, impacto e indicação de impedimento são campos complementares.
- No registro de humor, exigir clima/estado e permitir selecionar nuances compatíveis.
- Mostrar intensidade em escala visual de 1 a 10, com valor legível e rótulo textual.
- Usar a data selecionada como padrão e mostrar a data do registro no histórico.
- Listar registros com tipo, descrição/estado, intensidade e data; preservar informações opcionais no detalhe.
- Não usar rótulos ou cores que diagnostiquem ou julguem a pessoa.

#### FE-11 — Sinais vitais: formulário, lista e gráficos `[FORMULÁRIO/LISTA: MÍNIMO; GRÁFICOS: MÉDIO]`

##### Formulário de medição

- Permitir escolher: pressão arterial, glicose, batimentos cardíacos, saturação, temperatura, peso ou altura.
- Mostrar apenas os campos aplicáveis ao indicador selecionado.
- Pressão arterial deve solicitar sistólica e diastólica em campos separados, com unidade mmHg.
- Os demais indicadores devem solicitar o valor correspondente e exibir a unidade: glicose (mg/dL), batimentos (BPM), saturação (%), temperatura (°C), peso (kg) ou altura (cm).
- Solicitar data e horário da medição e oferecer observações opcionais.
- Recusar datas e horários futuros.
- Após salvar, atualizar a lista sem duplicar a medição; atualizar também o gráfico quando o recurso do escopo médio estiver habilitado.

##### Lista de medições

- Exibir uma linha por medição, ordenada da mais recente para a mais antiga.
- Mostrar indicador, valor e unidade; pressão deve exibir os dois valores, por exemplo “120/80 mmHg”.
- Mostrar data e horário, observações quando existentes e o perfil ao qual o registro pertence.
- Permitir filtrar por indicador e período sem misturar unidades ou tipos diferentes.
- Permitir abrir detalhes, editar e remover medições.
- Oferecer estado vazio com atalho para registrar a primeira medição e estado de erro separado de lista vazia.

##### Gráficos de evolução

- Apresentar seletor de indicador e intervalo: 7 dias, 30 dias, 90 dias ou período personalizado.
- Desenhar um gráfico temporal separado para cada indicador e perfil. Não combinar no mesmo eixo valores de unidades diferentes.
- Usar data no eixo horizontal e valor/unidade no eixo vertical. Em caso de várias medições no mesmo dia, manter os horários e permitir diferenciá-las.
- Para pressão arterial, desenhar duas séries claramente identificadas (sistólica e diastólica), com legenda; não reduzir o par a uma média.
- Para glicose, batimentos, saturação, temperatura, peso e altura, desenhar uma série por indicador. Se futuramente houver diferentes contextos de medição, como jejum, estes devem ser filtros/legendas, não dados misturados sem identificação.
- Ao tocar/focar um ponto, apresentar valor, unidade, data, horário e observações. Deve haver alternativa em tabela/lista acessível com os mesmos dados.
- Manter os pontos em ordem cronológica; não interpolar como se existissem medições em dias sem registro. Lacunas devem permanecer visíveis.
- Se houver poucos dados, mostrar os pontos disponíveis sem inventar uma tendência; se não houver dados, mostrar estado vazio e ação de registrar.
- Permitir que a seleção de período atualize gráfico e lista de forma consistente. A lista deve indicar o filtro aplicado e oferecer limpeza do filtro.
- Diferenciar séries também por marcador, traço ou rótulo, e não somente por cor. O gráfico deve ter descrição textual/alternativa acessível.
- Não exibir faixas de “normal”, risco, alertas clínicos ou diagnóstico sem fonte clínica validada e decisão explícita de produto. Se tais faixas forem adicionadas futuramente, devem estar rotuladas com fonte e contexto e não substituir avaliação profissional.
- Em celular, manter leitura e interação sem exigir rolagem horizontal do gráfico; permitir ampliar/consultar pontos sem esconder a lista.

**Critérios de aceitação dos gráficos**

1. Selecionar “Pressão arterial” mostra séries sistólica e diastólica, ambas em mmHg, com pontos e horários correspondentes.
2. Selecionar outro indicador substitui o gráfico e a unidade pelo indicador escolhido; valores de outro tipo não aparecem.
3. Alterar período atualiza pontos e lista para o mesmo intervalo.
4. Um perfil não visualiza medições de outro perfil.
5. Dias sem registros não são apresentados como medições nem como linha contínua de valores estimados.
6. Uma pessoa que não pode interpretar o gráfico visualmente ainda consegue consultar valores em lista/tabela acessível.

#### FE-12 — Consultas `[ESCOPO MÍNIMO]`

- O formulário deve coletar profissional/referência, especialidade, local, data, horário e observações.
- A lista deve apresentar profissional, especialidade, data e horário; local e observações devem estar disponíveis no detalhe.
- O calendário deve marcar a data da consulta.
- A tela deve permitir criar, editar, atualizar a lista e remover consultas.
- Se não houver consulta no dia selecionado, exibir mensagem de estado vazio e atalho para adicionar.

#### FE-13 — Exames `[ESCOPO MÍNIMO]`

- O formulário deve coletar nome, categoria, tipo de resultado, data e observações.
- A lista deve apresentar nome, categoria/tipo e data; observações devem permanecer acessíveis no detalhe.
- O calendário deve marcar a data do exame.
- Não oferecer botão para anexar laudo no escopo básico.

#### FE-14 — Documentos `[ESCOPO MÉDIO; ANEXOS OPCIONAIS]`

- Permitir cadastrar título, detalhes, data e observações.
- A lista deve distinguir documento descritivo de arquivo anexado.
- Não oferecer upload genérico de arquivos no escopo básico.
- Se anexos forem adicionados, apresentar tipo/tamanho aceitos, progresso, cancelamento e resultado do envio.

#### FE-15 — Saúde feminina e gestação `[ESCOPO MÉDIO]`

- Organizar separadamente acompanhamento de ciclo, registro diário e anticoncepcional.
- No ciclo, permitir registrar início, intensidade do fluxo e observações/sintomas; permitir encerrar com último dia válido.
- No registro diário, coletar data, fluxo, sintomas físicos, humor e observações, exigindo pelo menos uma informação além da data.
- Na tomada de anticoncepcional, coletar data, horário, confirmação de dose e observações.
- Exibir calendário/histórico e estimativas apenas quando houver dados suficientes.
- Manter o acompanhamento de gestação em fluxo separado.
- Identificar estimativas como aproximadas e nunca como método contraceptivo ou orientação médica.

#### FE-16 — Vacinas `[ESCOPO MÉDIO]`

- Coletar nome da vacina, data quando conhecida, opção explícita de data desconhecida e observações.
- Na lista, mostrar nome e data ou a indicação “data desconhecida”.
- Não converter data desconhecida em uma data inventada.
- Associar a carteira ao perfil ativo.

#### FE-17 — Perfis `[PERFIL PRINCIPAL: MÍNIMO; PERFIS DEPENDENTES: MÉDIO]`

- Exibir e identificar o perfil principal no escopo mínimo; listar e permitir alternar entre perfis dependentes no escopo médio.
- Permitir criar perfil dependente quando essa função estiver habilitada.
- Ao trocar de perfil, atualizar listas, calendário, resumo e formulários para o novo contexto.
- Se a troca de perfil falhar, manter o perfil anterior visível e informar o erro.

#### FE-18 — Configurações, Ajuda e backup `[CONFIGURAÇÕES/AJUDA: MÍNIMO; BACKUP: MÉDIO; SINCRONIZAÇÃO: OPCIONAL]`

- **Mínimo:** organizar as configurações de privacidade/segurança e a Ajuda; permitir pesquisar perguntas frequentes e expandir/recolher respostas.
- **Médio:** organizar preferências gerais e de acessibilidade; permitir alterar tema, cor e recursos de acessibilidade; oferecer exportação/importação local de backup, explicar o conteúdo incluído, excluir token de sessão e confirmar operações destrutivas ou substituições.
- **Opcional:** apresentar configurações de IA e sincronização remota de backup somente quando aplicáveis.
- Exibir claramente quais opções dependem de conta, permissões ou serviço externo.
- “Apagar dados locais” deve ser separado de sair da conta, explicar o que será removido e exigir confirmação.

### 8.5 Regras de formulários e feedback

#### FE-19 — Comportamento comum dos formulários `[ESCOPO MÍNIMO — TRANSVERSAL]`

- Usar a data selecionada no calendário como valor inicial quando o formulário trabalha com data e não houver outra data fornecida.
- Marcar visualmente campos obrigatórios e opcionais.
- Mostrar erros junto ao campo correspondente e preservar os valores já preenchidos após erro de rede/API.
- Desabilitar o botão de salvar durante o envio para evitar duplicidade.
- Informar sucesso somente após persistência local Guest ou confirmação da API.
- Permitir fechar/cancelar sem salvar e avisar sobre perda de dados se houver alterações não salvas.
- Respeitar teclado e controles acessíveis, com rótulos associados aos campos.

#### FE-20 — Estados e mensagens `[ESCOPO MÍNIMO — TRANSVERSAL]`

Cada tela que carrega dados deve distinguir:

- **Carregando:** solicitação em andamento.
- **Vazio:** carregamento concluído sem registros.
- **Erro:** solicitação falhou; permitir nova tentativa quando apropriado.
- **Offline/pendente:** informar o uso local Guest no mínimo; indicar operações autenticadas aguardando sincronização quando a fila do escopo médio estiver habilitada.
- **Conteúdo carregado:** dados apresentados com o contexto de perfil/data.

Uma lista vazia não deve ser apresentada enquanto o carregamento estiver em andamento nem quando a API retornou erro.

### 8.6 Requisitos visuais, responsivos e acessíveis

#### FE-21 — Responsividade `[ESCOPO MÍNIMO — TRANSVERSAL]`

- Em telas largas, utilizar navegação lateral e conteúdo em colunas/cartões quando isso facilitar a leitura.
- Em telas estreitas, reorganizar cartões, formulários, listas e gráficos em uma coluna sem cortar conteúdo ou ações.
- Tabelas/listas extensas devem continuar legíveis em celular; ações não podem depender de hover.
- Modal e calendário devem caber na área visível e permitir rolagem sem esconder botões de confirmação/cancelamento.

#### FE-22 — Acessibilidade visual e de interação `[ACESSIBILIDADE BÁSICA: MÍNIMO; OPÇÕES AVANÇADAS: MÉDIO]`

- Não usar somente cor para representar estado, tipo de registro, seleção ou série de gráfico.
- Exibir foco visível na navegação por teclado e permitir alcançar ações sem mouse.
- Fornecer nome acessível a botões com ícone e rótulos para campos.
- Manter contraste suficiente no mínimo; oferecer ampliação de texto e redução de animações no escopo médio.
- Para gráficos do escopo médio, disponibilizar legenda, descrição textual e tabela/lista equivalente.
- Mensagens de erro, sucesso e sincronização devem ser anunciadas de forma acessível e não depender apenas de animação.

### 8.7 Esquema de cores e identificação visual

#### FE-23 — Identidade da marca Katu'I `[ESCOPO MÍNIMO]`

- Utilizar o nome **Katu'I** com a grafia e pontuação consistentes em telas, título do navegador, manifesto e materiais de ajuda.
- Utilizar a marca gráfica oficial Katu'I, usando `frontend/publico/logo-katui.png` como referência, em áreas de identificação como tela inicial, cabeçalho/menu e ícone do aplicativo Web.
- Exibir a assinatura **“cuidado que acompanha”** junto à marca quando houver espaço, sem substituir o nome do produto.
- A imagem da marca deve ter texto alternativo “Katu'I” quando for informativa. Se for apenas decorativa e o nome já estiver adjacente, evitar leitura duplicada por leitor de tela.
- Manter proporção, nitidez e área de respiro da marca; não esticar, recortar, recolorir ou aplicar efeitos que prejudiquem sua leitura.
- Usar a marca e a paleta institucional nos pontos globais da aplicação. Cores personalizáveis do perfil são acentos por pessoa/perfil e não devem substituir a identificação institucional do produto.

#### FE-24 — Paleta institucional `[MÍNIMO; MODO ESCURO: MÉDIO]`

As cores abaixo definem a paleta institucional a ser usada para manter consistência entre navegação, botões, estados e instalação Web:

| Papel visual | Cor | Aplicação |
|---|---|---|
| Verde-petróleo institucional | `#176B62` | Cor de marca, navegação ativa, links e ações primárias. |
| Verde da marca | `#2C9181` | Elementos gráficos e destaques secundários da marca. |
| Verde claro da marca | `#6DB8A6` | Detalhes gráficos e realces suaves. |
| Verde muito claro da marca | `#9BCDC0` | Elementos gráficos complementares, sem uso isolado para texto pequeno. |
| Fundo principal | `#F7F9F7` | Fundo geral das telas e da instalação Web. |
| Fundo alternativo/base | `#F6F8F6` | Variação de fundo para as telas. |
| Texto principal | `#213836` | Texto corrido e conteúdo principal. |
| Título de página | `#254A43` | Títulos de página. |
| Fundo de seleção/realce | `#E8F4F0` | Item de navegação ativo e blocos de resumo. |
| Borda de separação | `#E7ECE9` | Divisórias e limites sutis de componentes. |

Regras de aplicação:

- Preservar o verde-petróleo como cor de reconhecimento principal; não atribuir o mesmo significado a diversas cores de destaque sem necessidade.
- Usar fundos claros e texto escuro como padrão, mantendo contraste legível em textos, controles e estados.
- Usar o fundo de seleção para indicar contexto ativo, como navegação selecionada; não depender exclusivamente dele para comunicar seleção.
- Cores de sucesso, aviso, erro e informação devem ter rótulo/ícone e contraste adequados, além da cor.
- Não usar a paleta de status para classificar dados clínicos nem para sugerir diagnóstico.
- Definir no manifesto do Site uma cor de tema alinhada à cor institucional (`#176B62`), salvo mudança de identidade aprovada.
- No escopo médio, oferecer modo escuro adaptando fundo, texto, bordas, estados e cores da marca; preservar contraste e reconhecimento, sem simplesmente inverter todas as cores.

#### FE-25 — Temas de cor dos perfis `[ESCOPO MÉDIO]`

O Site deve oferecer opções de cor associadas ao perfil. Elas servem para personalizar realces e identificar o perfil ativo, sem alterar a paleta global da marca.

| Tema | Cor principal | Fundo suave | Variante escura | Cor do texto sobre a cor principal |
|---|---|---|---|---|
| Turquesa | `#009C9C` | `#E2F5F3` | `#66C8C5` | `#FFFFFF` |
| Dourado | `#D89A27` | `#FFF1CD` | `#F0B94F` | `#4A3510` |
| Azul | `#2F8FB0` | `#E5F3F8` | `#80C5D9` | `#FFFFFF` |
| Violeta | `#7054A5` | `#EEE9F7` | `#B8A5D7` | `#FFFFFF` |
| Rosa | `#C66B88` | `#F9E8EE` | `#E4A6B9` | `#FFFFFF` |
| Verde | `#4A9A73` | `#E6F4ED` | `#91D1B2` | `#FFFFFF` |

- Apresentar os temas com nome textual, amostra de cor e estado selecionado perceptível por forma/ícone/rótulo, não apenas por cor.
- Aplicar o tema ao perfil atualmente ativo e evitar que a troca altere os registros de saúde.
- Restaurar a opção correspondente ao perfil ao entrar ou alternar perfis; se a preferência estiver ausente ou inválida, usar o tema padrão turquesa.
- Garantir legibilidade de texto e foco interativo sobre cada cor. A cor `ink`/texto definida para o tema deve ser respeitada ou substituída apenas por combinação com contraste verificado.
- No modo escuro, usar a variante escura apropriada quando necessário e validar novamente o contraste do texto e dos controles.

#### FE-26 — Tipografia e hierarquia visual `[ESCOPO MÍNIMO]`

- Utilizar **DM Sans** como fonte principal da interface e **Playfair Display** nos títulos e destaques editoriais; manter alternativas de sistema caso as fontes externas não carreguem.
- Preservar hierarquia entre título de página, título de seção, texto de apoio, conteúdo de registro, rótulo e mensagem de estado.
- Usar a tipografia serifada de destaque em títulos principais; conteúdo funcional e formulários devem manter alta legibilidade.
- Não reduzir textos essenciais, metadados ou instruções a tamanhos que prejudiquem leitura em telas menores.
- As opções de tamanho de texto/acessibilidade do escopo médio devem ampliar conteúdo sem sobreposição ou corte de botões, listas e gráficos.

#### FE-27 — Componentes e consistência visual `[ESCOPO MÍNIMO]`

- Manter padrões consistentes de cartões, botões, campos, modais, ícones, listas, calendário e estados vazios entre módulos.
- Reservar a cor institucional para ações primárias e estados de navegação; ações secundárias devem permanecer visualmente distintas.
- Ícones devem seguir estilo e escala consistentes, vir acompanhados de rótulos quando a ação não for universalmente reconhecível e ter nome acessível.
- Cantos, espaçamentos, bordas, sombras e estados de foco/hover devem manter consistência entre os componentes.
- Aplicar marca e cores tanto na interface quanto nos metadados do Site instalável, evitando que o navegador/PWA apresente identificação visual conflitante.

**Critérios de aceitação da identidade visual**

1. Nome, logotipo e assinatura são consistentes nas telas principais e na identificação do Site.
2. Verde-petróleo e fundos institucionais permanecem consistentes entre as telas, inclusive em estados ativos e botões.
3. Alterar a cor do perfil muda apenas os realces daquele perfil e não substitui a marca global.
4. Textos e controles continuam legíveis em todos os seis temas e no modo escuro.
5. Estados importantes e seleções são compreensíveis sem depender exclusivamente da cor.
6. A ausência de carregamento das fontes externas não impede o uso nem compromete a hierarquia/legibilidade.

---

## 9. Requisitos complementares por escopo `[MÉDIO E OPCIONAL]`

- **Médio — Sinais vitais:** implementar gráficos históricos com filtros, detalhes e alternativa acessível conforme FE-11.
- **Médio — Medicamentos:** calcular resumos e estoque a partir dos registros do perfil; indicar ausência de dados em vez de exibir totais fictícios.
- **Médio — Perfis dependentes:** oferecer ações de administração dos perfis dependentes, respeitando a autorização da conta.
- **Médio — Documentos:** manter cadastro descritivo sem upload de arquivos; o cadastro de exames pertence ao mínimo e não deve incluir upload de laudos.
- **Opcional — Integrações:** apresentar leitura automatizada de receitas e notificações Push como disponíveis somente quando configuradas e funcionais.
- **Médio — Identidade visual:** verificar contraste da paleta e dos temas de perfil em textos, focos e controles nos modos claro e escuro.

---

## 10. Referências do projeto

- [Guia funcional do aplicativo](./frontend/documentacao/guia-do-aplicativo.md) — telas, campos, relacionamentos e limitações.
- [Contrato da API](./frontend/documentacao/contrato-api.md) — rotas, autenticação, payloads e sincronização.
- [README do projeto](./README.md) — visão geral e instruções para executar o projeto.
