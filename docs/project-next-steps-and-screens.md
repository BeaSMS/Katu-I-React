# Próximos passos e inventário de telas do Katu'I

Este documento ajuda novas colaboradoras a entender o estado conhecido do projeto, escolher trabalho seguinte e alinhar a implementação da API principal com as telas existentes. O inventário descreve o que o frontend atualmente coleta ou espera; ele **não confirma** que a API principal já implementa esses contratos. Antes de iniciar integração, compare com o backend real.

## Próximos passos do projeto

### Prioridade 0 — Desbloquear execução integrada

- [ ] **Localizar e documentar a API principal.** Adicionar o repositório/caminho oficial, responsável, instruções para instalação e inicialização, variáveis de ambiente e endereço de desenvolvimento. O frontend atual assume `http://localhost:3000/api`, mas este repositório não inicia essa API.
- [ ] **Conferir o contrato frontend ↔ API.** Verificar cada rota, método, payload, formato de resposta, autenticação, perfil selecionado, erros e `Idempotency-Key` contra a implementação real; atualizar `docs/api-contract.md` com divergências decididas.
- [ ] **Fornecer um caminho seguro para testes.** Definir como obter credenciais e dados de desenvolvimento (contas sem dados reais, seed/fábrica de dados ou ambiente compartilhado protegido). Nunca adicionar credenciais, tokens ou dados pessoais reais ao repositório.
- [ ] **Validar o Electron com API real.** Testar login, `/me`, onboarding, leitura/escrita de registros, armazenamento local e logout no instalador. O Electron empacotado carrega a interface via `file://`; definir uma estratégia de origem/CORS segura em vez de liberar `Origin: null` globalmente.

### Prioridade 1 — Confiabilidade e privacidade

- [ ] **Automatizar CI para pull requests.** Executar `npm ci`, `npm test` e `npm run build` em ambiente limpo; documentar e corrigir falhas antes de integrar.
- [ ] **Adicionar testes de integração/E2E dos fluxos principais.** Cobrir registro/login, onboarding e perfis, CRUD de registros, troca de perfil, modo Guest, fila offline e recuperação de rede. Para Push, testar autenticação, validação do perfil, agendamento, remoção e falhas da API principal.
- [ ] **Acordar política de dados de saúde.** Especificar minimização, consentimento, acesso por perfil, retenção, exportação, restauração e exclusão de conta/dados. Verificar criptografia em trânsito/em repouso, logs sem dados sensíveis, backups e requisitos legais aplicáveis antes de uso com dados reais.
- [ ] **Revisar limites e comportamento da fila offline.** Confirmar como conflitos, duplicatas, ações pendentes inválidas, logout, troca de perfil e falhas parciais são apresentados e recuperados.
- [ ] **Revisar contratos de arquivos e IA.** Definir tipos/tamanhos aceitos, armazenamento/expiração de documentos, resposta da IA e confirmação explícita; manter sugestões sem salvar medicamentos automaticamente.

### Prioridade 2 — Produto e manutenção

- [ ] **Definir suporte e distribuição desktop.** Escolher versões de Windows/macOS/Linux suportadas, assinatura/notarização, canal e política de atualização, requisitos de rede e processo de reporte de falhas.
- [ ] **Manter uma lista priorizada de issues.** Cada tarefa deve ter responsável, critérios de aceite, telas/rotas afetadas e dependências; separar defeitos, dívida técnica e funcionalidades futuras.
- [ ] **Atribuir revisão e fluxo de contribuição.** Definir nomes/papéis de revisores, convenção de branches/commits e requisitos para pull request; substituir no README os passos genéricos pelo processo real do grupo.
- [ ] **Revisar acessibilidade e UX com pessoas usuárias.** Testar teclado, leitores de tela, escalas de texto, mensagens de erro, estados vazios/carregamento/offline e fluxos de saúde feminina com usuárias diversas.
- [ ] **Validar conteúdo clínico.** Textos, lembretes e recomendações devem ser revisados por profissional qualificado; não apresentar estimativas do ciclo ou saídas de IA como diagnóstico ou instrução médica.

## Telas e dados esperados

### Como interpretar o inventário

- Os nomes de campo abaixo correspondem ao frontend atual e aos contratos documentados.
- `profileId` deve ser associado ao perfil ativo nas chamadas por perfil; o backend deve verificar a autorização, não confiar somente no header.
- Datas de registros sem horário usam `YYYY-MM-DD`; horários locais usam `HH:mm`; horários absolutos de agendamento Push são ISO 8601.
- Campos podem ser opcionais conforme a tela/fluxo. Os campos obrigatórios indicados são os mínimos exigidos pela validação atual da interface, não necessariamente uma validação clínica.
- O frontend tem telas com placeholders/estados vazios. Quando indicado, confirmar o comportamento esperado antes de implementar uma API mais ampla.

### Acesso, criação de conta e perfil inicial

| Tela/fluxo | Dados coletados ou apresentados | Destino/comportamento esperado |
|---|---|---|
| Boas-vindas / entrada | Nenhum campo | Oferece login, registro ou modo Guest. |
| Login | `email`, `password` | `POST /auth/login`; resposta com `token` ou `accessToken`, seguida de `GET /me`. |
| Criar conta | `name`, `email`, `password`, confirmação local da senha | `POST /auth/register`; confirmação de senha não precisa ser enviada. Se houver dados Guest, opção explícita de consentimento para importá-los. |
| Modo Guest | Nenhum campo de conta | Dados permanecem locais; sem IA, sincronização ou backup em nuvem. |
| Onboarding do perfil principal | `name`, `age`, `sex`, `weight`, `heightCm`, `allergies`, `specialConditions`, `aiEnabled`, `themeColor`, `corHex`, `preferences` | `POST /profiles/me`; o servidor retorna ID canônico. Campos pessoais opcionais podem ser nulos. Guest salva um perfil local. |

### Navegação e telas principais

| Tela | Dados que lê/mostra | Ações e dados enviados |
|---|---|---|
| Visão geral | `GET /overview`: resumo de `medications`, `vitalSigns`, `appointments`, `symptoms`; calendário consulta medicamentos, alarmes, receitas, sintomas, sinais vitais, consultas e exames para marcar dias ativos. | Atalhos para novo sintoma, medicamento, receita, exame e outros registros; seleção de data altera o recorte exibido. |
| Calendário global | Registros com `date`, ou `startDate`/`endDate` para intervalos como tratamentos. | Seleção de dia/mês; filtra módulos pela data. Não é uma coleção própria do backend. |
| Seletor de perfil | Lista `profiles` com `id`, `name`, `relation` e preferências associadas. | Troca `X-Profile-Id`; adicionar dependente abre formulário de perfil; sair remove a sessão local no frontend atual. |
| Ajuda | Conteúdo FAQ estático e filtro textual local. | Busca local; não requer endpoint próprio. |

### Módulos e registros de saúde

As telas de coleção carregam registros do perfil ativo, filtram pela data selecionada e mostram estado vazio/erro. Em geral usam `GET /<coleção>`, `POST /<coleção>`, `PATCH /<coleção>/:id` e `DELETE /<coleção>/:id`, salvo rotas especiais listadas.

| Tela / endpoint da API principal | Dados esperados no registro |
|---|---|
| **Medicamentos** `/medications` | `name`, `dosage`, `purpose`, `date`/`startDate`, `time`, `frequencyType`, `frequencyValue`, `continuous`, `durationDays`, `quantity`, `minimumStock`, `dosePerTake`, `unit`, `generateAlarms`. `generateAlarms` pode criar/remover agenda no serviço Push web. |
| **Anticoncepcional** `/medications` e `/feminine-health` | Configuração de medicamento: `name`, `date`, `time`, `contraceptivePackType` (`21`, `24`, `28` ou `continuous`), `contraceptiveActiveDays`, `contraceptivePauseDays`; o frontend deriva `purpose`, `dosage`, frequência e uso contínuo. Registro de dose: `recordType: "contraceptive-dose"`, `medicationId`, `date`, `time`, `taken`, `notes`. |
| **Alarmes** `/alarms` | Formulário principal: `name`, `medication`, `date`, `time`, `notes`. Para Push web, o serviço mantém agenda e vincula `sourceId`; a lista Push pode mostrar `nextAt`, `intervalMinutes`, `durationDays`, estado e origem. |
| **Receitas** `/prescriptions` | Cadastro manual: `name`, `date`, `notes`. A tela de leitura envia arquivo (`file`) e observações para `POST /ai/prescription`; a resposta precisa conter sugestão revisável, incluindo medicamentos e avisos. |
| **Confirmação da receita** `/prescriptions/:id/confirm` | `{ medications: [...] }` com sugestões explicitamente selecionadas; cada medicamento pode incluir posologia e configuração de alarme. Só após confirmação devem ser persistidos; resposta inclui IDs canônicos dos medicamentos criados. |
| **Sinais vitais** `/vital-signs` | `type`, `date`, `time`, `notes`; para pressão: `systolic`, `diastolic`; para glicose, batimentos, saturação, temperatura, peso e altura: `value` com unidade correspondente. |
| **Diário de sintomas** `/symptoms` | Físico: `type: "FISICO"`, `quality`, `location`, `duration`, `factors`, `impact`, `intensity` (1–10), `incapacitating`, `date`. Humor: `type: "PSICOLOGICO"`, `climate` (Positivo/Neutro/Baixo/Tenso), `nuances`, `factors`, `intensity`, `date`; o frontend também usa `name`/`quality` para rótulo. |
| **Consultas** `/appointments` | `name` (profissional), `detail` (especialidade), `location`, `date`, `time`, `notes`. |
| **Exames** `/exams` | `name`, `category` (Outros/Laboratório/Imagem/Cardiologia/Endoscopia), `examType` (Imagem única/Série temporal/Resultado numérico), `date`, `notes`. |
| **Documentos** `/documents` | Tela organizada para documentos de saúde. O formulário atual usa campos genéricos `name`, `detail`, `date`, `notes`; confirmar com o grupo se upload de arquivo é requisito e definir limites/formato antes de implementar armazenamento. |
| **Saúde feminina — ciclo** `/feminine-health` | `recordType: "menstrual-cycle"`, `date`/`startDate`, `endDate` (quando finalizado), `flow`, `physicalSymptoms`. Criação inicia ciclo; `PATCH /feminine-health/:id` encerra. |
| **Saúde feminina — diário** `/feminine-health` | `recordType: "daily-symptom"`, `date`, `flow` (Sem fluxo/Leve/Média/Intensa), `physicalSymptoms`, `mood`, `notes`; um registro do dia pode ser editado. |
| **Saúde feminina — gestação** `/pregnancy` | `enabled`, `startDate`, `profileId`. O estado também é mantido localmente por perfil; alinhar com backend qual fonte é autoritativa e como sincronizar. |
| **Vacinas** `/vaccines` | `name`, `date`, `unknownDate`, `notes` (incluindo lote/local quando fornecidos). |
| **Relatórios** `/reports` | A tela oferece criação de relatório PDF. `POST /reports` cria e `GET /reports/:id/download` retorna `application/pdf`. |
| **Perfis e dependentes** `/profiles` | `name`, `relation`, `age`, `sex`, `weight`, `heightCm`, `allergies`, `specialConditions`, `themeColor`, `corHex`, `preferences`. Alteração de cor usa `PATCH /profiles/:id`. |

### Configurações

As configurações são divididas por abas; em geral são aplicadas ao perfil ativo:

| Aba | Preferências/dados |
|---|---|
| Preferências gerais | `darkMode`, `notificationsEnabled`, `weeklySummaryEnabled`, feedback sonoro local; notificações locais; ativação/desativação de Web Push na versão web. |
| Inteligência artificial (conta) | `provider` (`system`, `openai`, `google`, `anthropic`), `model`, `apiKey` enviada somente ao salvar. Usa `PATCH /profiles/:id/ai-settings`; a chave não deve ser persistida no navegador ou na fila offline. |
| Acessibilidade | `accessibility`, incluindo escala de texto/tamanho ampliado e opções disponíveis na interface. |
| Privacidade e segurança | Exportar/importar backup local, limpar dados locais e informações de proteção/armazenamento. |
| Backup pessoal (conta) | `POST /backup` para sincronização manual; exportação local e importação de arquivo de backup. |
| Tema do perfil | `themeColor` e `corHex`; `PATCH /profiles/:id`. |

### Formato geral esperado para coleções

O identificador retornado para cada perfil/registro deve ser estável e canônico (`id`; compatibilidade com `_id` existe em alguns pontos do frontend). A API deve retornar arrays JSON nas listagens, objetos nos writes e `204 No Content` em exclusões. Filtre e autorize dados pelo usuário e perfil no servidor. Campos comuns podem incluir `profileId`, datas, `status`, `name`/`title` e `detail`, mas cada coleção deve preservar seus campos específicos acima.

Para requisitos de resposta, autenticação, formatos de arquivo, sincronização offline e detalhes de IA, consulte [`api-contract.md`](./api-contract.md). Para alarmes Push, consulte [`push-service.md`](./push-service.md).
