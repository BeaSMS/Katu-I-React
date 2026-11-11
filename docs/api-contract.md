# Contrato da API principal do Katu'I

## Arquitetura e dependências

Este documento descreve a **API principal** de contas e dados, configurada pela variável `VITE_API_URL`. Essa API é um serviço separado: não está implementada neste repositório e precisa estar rodando localmente durante o desenvolvimento (padrão `http://localhost:3000/api`) ou implantada e acessível em produção.

O projeto inclui outro processo Node, `server/push-server.js`, destinado ao Web Push. Ele é complementar, valida tokens chamando `${AUTH_API_URL}/me` na API principal e tem contrato separado em [`push-service.md`](./push-service.md).

O frontend usa `VITE_API_URL` como base e acrescenta os caminhos deste contrato. Ele envia o token salvo em `katui.auth-token` no header `Authorization: Bearer <token>` e, quando aplicável, o perfil selecionado em `X-Profile-Id`. Como as variáveis `VITE_*` são incorporadas no bundle durante o build, configure a URL antes de criar o build web.

## Convenções

- Respostas de listagem: arrays JSON.
- Respostas de criação/edição: objeto JSON criado ou atualizado.
- Exclusão: `204 No Content`.
- Erros: `{ "message": "...", "code": "..." }` com status HTTP adequado.
- Somente falhas de rede e timeout permitem salvar uma ação comum na fila offline. Erros HTTP de validação/autorização são mostrados e não são convertidos em sucesso local.

## Saúde e sessão

| Método | Endpoint | Uso |
|---|---|---|
| GET | `/health` | Verificar disponibilidade do servidor |
| GET | `/overview` | Resumo da página inicial |
| POST | `/auth/register` | Criar conta |
| POST | `/auth/login` | Iniciar sessão |
| GET | `/me` | Dados do usuário autenticado, `profiles` com IDs canônicos e `activeProfileId` opcional |

O frontend atual encerra a sessão localmente removendo o token e não chama `POST /auth/logout`; o backend pode oferecer essa rota se precisar invalidar tokens no servidor, mas ela não é necessária para o fluxo atual.

`POST /profiles/me` e `POST /profiles` devem retornar o perfil criado com um `id` canônico (diretamente ou em `profile.id`). O frontend usa esse ID nas chamadas seguintes. O endpoint `/me` retorna `{ "profiles": [...], "activeProfileId": "..." }`; a lista pode estar vazia se o onboarding ainda não foi concluído.

`GET /overview` retorna `{ "medications": [], "vitalSigns": [], "appointments": [], "symptoms": [] }`. As listas usam os mesmos objetos dos respectivos endpoints de recurso e incluem as datas necessárias para filtrar registros e indicar atividade no calendário. Para recursos com ocorrência em intervalo (como medicamentos), informe `startDate` e `endDate` no formato `YYYY-MM-DD`; para registros pontuais, informe `date` no mesmo formato. O frontend também reconhece campos de data/hora ISO retornados pelas telas nativas.

As listas de `GET /medications`, `/alarms`, `/prescriptions`, `/vital-signs`, `/appointments`, `/exams` e `/symptoms` devem permitir recuperar os registros do perfil com suas datas; o frontend usa esse conjunto para marcar os dias com atividade e exibe apenas os registros do dia selecionado.

## Recursos do Katu'I

As coleções de dados abaixo são lidas por `GET /colecao`, criadas por `POST /colecao` e, onde há edição/remoção, usam `PATCH /colecao/:id` e `DELETE /colecao/:id`. As telas chamam as rotas à medida que a pessoa navega ou faz ações; não é necessário que cada rota seja exercitada em toda sessão.

| Recurso | Endpoint |
|---|---|
| Medicamentos e estoque | `/medications` |
| Alarmes | `/alarms` |
| Receitas | `/prescriptions` |
| Sinais vitais | `/vital-signs` |
| Diário de sintomas | `/symptoms` |
| Saúde feminina | `/feminine-health` |
| Gestação | `/pregnancy` |
| Consultas | `/appointments` |
| Vacinas | `/vaccines` |
| Exames | `/exams` |
| Documentos | `/documents` |
| Relatórios | `/reports` |
| Perfis e dependentes | `/profiles` |

Rotas especiais adicionais:

| Método | Endpoint | Observação |
|---|---|---|
| POST | `/profiles/me` | Cria o perfil principal durante o onboarding |
| PATCH | `/profiles/:id/ai-configuracoes` | Atualiza configuração de IA daquele perfil |
| POST | `/ai/prescription` | Leitura de receita por IA (`multipart/form-data`, campo `file` para novo envio ou `prescriptionId` para usar um anexo já salvo) |
| POST | `/prescriptions` | Cadastra receita como JSON ou `multipart/form-data` quando há anexo opcional (PDF, JPG ou PNG, até 15 MB) |
| POST | `/prescriptions/:id/confirm` | Confirma sugestões revisadas para a receita `:id` e cria os medicamentos selecionados; recebe `analysisId` e `medications` |
| POST | `/exams` | Cadastra exame como `multipart/form-data`; campo `file` obrigatório (PDF, JPG ou PNG, até 15 MB), além dos campos do registro |
| POST | `/documents` | Cadastra documento como JSON quando sem arquivo ou `multipart/form-data` quando há anexo opcional (PDF, JPG ou PNG, até 15 MB) |
| GET | `/reports/:id/download` | Retorna PDF (`application/pdf`) |
| POST | `/backup` | Solicita sincronização/backup pessoal; exige conta |

Quando houver arquivo, as respostas de `/exams`, `/documents` e `/prescriptions` devem incluir `attachment` com `name`, `contentType`, `size` e `downloadUrl`. O link deve ser acessível pelo usuário autorizado (por exemplo, uma URL assinada de curta duração). Para a análise por ID, `/ai/prescription` deve buscar o anexo da receita autorizada, sem exigir novo upload, e incluir `prescriptionId` na resposta. A confirmação atualiza a receita existente e cria somente os medicamentos selecionados; a resposta deve incluir os medicamentos criados com `id` e o `clientId` recebido, para que o app possa vincular cada alarme ao medicamento correto. No modo Guest, o app mantém o anexo localmente no armazenamento criptografado. Envios com arquivo que aguardam sincronização ficam na fila offline criptografada.

`/backup` não deve ser tratado como uma coleção CRUD neste frontend: a ação atual envia `POST /backup`. A API também pode fornecer `GET /reports` e `POST /reports` para listar/criar relatórios.

## Sincronização offline

O frontend mantém ações comuns pendentes localmente somente quando a API falha por rede/timeout. A fila é armazenada em IndexedDB e criptografada com AES-GCM. Se IndexedDB ou Web Crypto não estiver disponível, o salvamento offline falha visivelmente; dados de saúde não são gravados em texto puro no `localStorage`. A fila legada em `localStorage`, de versões anteriores, é migrada para IndexedDB criptografado antes de ser removida. Cada tentativa online recebe um `Idempotency-Key` estável, reutilizado se a resposta se perder e a operação precisar ser enfileirada. Erros de cliente (HTTP 4xx, exceto 401/403) ficam marcados para atenção e não impedem a sincronização das ações seguintes. Erros 401/403, 5xx e de rede interrompem o envio sem remover a ação.

Dados Guest, perfis locais e rascunhos de onboarding também são migrados de `localStorage` para um IndexedDB próprio e criptografados com AES-GCM. A chave não-extraível é armazenada no IndexedDB do mesmo navegador, portanto essa proteção não resiste a XSS ou ao acesso ao perfil completo do navegador. A migração remove as cópias legadas de `localStorage` somente depois que a gravação criptografada é confirmada; se o navegador não oferecer IndexedDB ou Web Crypto, o app não inicia e os dados legados permanecem intactos.

## Autenticação

As telas públicas usam `POST /auth/register` com `name`, `email` e `password`, e `POST /auth/login` com `email` e `password`. A resposta deve retornar um token em `token` ou `accessToken`; o frontend o envia como `Authorization: Bearer` nas chamadas seguintes. Após autenticar, o frontend consulta `GET /me` para recuperar os perfis e evitar depender apenas de dados do navegador. O backend deve aplicar hash de senha, limitar tentativas, validar e-mail e devolver mensagens genéricas para credenciais inválidas.

O backend deve processar `Idempotency-Key` de forma idempotente, usar HTTPS, autenticar o usuário e validar novamente `profileId`, permissões e payload. O frontend não considera o dado sincronizado até receber uma resposta HTTP de sucesso. A fila é reprocessada ao evento `online` e também durante as verificações periódicas de conexão. Ela é vinculada ao dispositivo e não é incluída no backup JSON exportado, para evitar transformar dados pendentes criptografados em texto puro; apagar os dados locais também limpa essa fila.

O tutorial inicial envia o perfil principal para `POST /profiles/me` com os campos `name`, `age`, `sex`, `weight`, `heightCm`, `allergies`, `specialConditions` e `aiEnabled`. O backend deve aceitar `Idempotency-Key` também nessa criação, pois ela pode ser repetida após um timeout. Se o perfil for criado offline, o frontend troca o ID temporário pelo ID retornado ao sincronizar e atualiza as ações pendentes que dependem dele.

A preferência de IA é por perfil. `PATCH /profiles/:id/ai-configuracoes` recebe `provider` (`system`, `openai`, `google` ou `anthropic`), `model` opcional e `apiKey` somente quando a pessoa escolher um provedor próprio. O frontend não persiste a chave no navegador nem a coloca na fila offline; o backend deve armazená-la em um cofre/secret manager, criptografada e associada ao usuário. Para `provider: system`, o backend usa a chave padrão configurada no servidor.

O cadastro de dependentes usa `POST /profiles` com `name`, `relation`, `age`, `sex`, `weight`, `heightCm`, `allergies`, `specialConditions`, `themeColor`, `corHex` e `preferences`. Alterações em dependentes e preferências do perfil usam `PATCH /profiles/:id`; o payload de preferências contém `preferences` com `darkMode`, `notificationsEnabled`, `weeklySummaryEnabled` e `accessibility`. A preferência de cor também usa essa rota com `themeColor` e `corHex`. `themeColor` identifica uma opção da paleta (`turquoise`, `gold`, `sky`, `purple`, `rose` ou `green`) e `corHex` mantém a cor derivada para compatibilidade com o APK original. As preferências são armazenadas localmente e sincronizadas com a conta, inclusive pela fila offline.

## Dados dos formulários

Os cadastros enviam também `profileId` e `date` (a data escolhida no calendário global) quando aplicável. Os campos específicos usados pelo frontend são:

- Medicamentos: `name`, `dosage`, `purpose`, `time`, `frequencyType`, `frequencyValue`, `continuous`, `durationDays`, `quantity`, `minimumStock`, `dosePerTake`, `unit`, `generateAlarms`. O cadastro dedicado de anticoncepcional usa o endpoint `/medications`, com `purpose: "Anticoncepcional"`, e acrescenta `contraceptivePackType` (`21`, `24`, `28` ou `continuous`), `contraceptiveActiveDays` e `contraceptivePauseDays`. Para cartelas de 21/24 dias, o agendador Web Push recebe `cycleActiveDays` e `cyclePauseDays` e suspende lembretes durante a pausa; o registro continua integrado à lista geral de medicamentos e aos alarmes.
- Alarmes: `name`, `medication`, `date`, `time`, `notes`.
- Receitas: `file`, `date`, `notes` na leitura por IA; `name`, `date`, `notes` no cadastro manual.
- Sinais vitais: `type`, `value`, `systolic`, `diastolic`, `date`, `time`, `notes`.
- Consultas: `name` (médico), `detail` (especialidade), `location`, `date`, `time`, `notes`.
- Exames: `name`, `category`, `examType`, `date`, `notes`.
- Sintomas: `name`, `type` (`FISICO` ou `PSICOLOGICO`), `quality`, `location`, `duration`, `factors`, `impact`, `intensity`, `incapacitating`, `climate`, `nuances`, `date`.
- Saúde feminina (`/feminine-health`): registros distinguem-se por `recordType`:
  - `menstrual-cycle`: `startDate`, `endDate` (opcional enquanto o fluxo estiver em andamento), `date`, `flow` e `physicalSymptoms`. O frontend cria o ciclo com `POST` e encerra o mesmo recurso com `PATCH /feminine-health/:id`.
  - `daily-symptom`: `date`, `flow`, `physicalSymptoms`, `mood` e `notes`.
  - `contraceptive-dose`: `medicationId`, `date`, `time`, `taken` e `notes`.
  Todos os registros devem aceitar `profileId` e usar datas `YYYY-MM-DD`. As estimativas de ciclo são calculadas no frontend a partir dos ciclos registrados; não substituem aconselhamento profissional nem devem ser usadas como contracepção.
- Vacinas: `name`, `date`, `unknownDate`, `notes`.

## IA e arquivos

`POST /ai/prescription` recebe `multipart/form-data` com o campo `file` e retorna uma sugestão revisável:

```json
{
  "id": "prescription_123",
  "status": "review",
  "medications": [
    { "name": "Losartana", "dose": "50 mg", "schedule": "08:00" }
  ],
  "warnings": []
}
```

O frontend não deve salvar automaticamente as sugestões da IA: a tela de revisão permite selecionar as sugestões e a confirmação da pessoa usuária chama `POST /prescriptions/:id/confirm` com `{ "medications": [...] }`. A leitura da receita exige conexão e seu arquivo não é colocado na fila offline.

O botão de leitura assistida só é exibido quando o build define `VITE_AI_PRESCRIPTION_ENABLED=true`; em modo de demonstração ele apresenta uma resposta fictícia identificada como demonstração. Sem essa configuração, o cadastro manual de receitas continua disponível.

Se a pessoa configurar alarmes para um medicamento nessa revisão, o objeto enviado também inclui `date`, `time`, `frequencyType`, `frequencyValue`, `continuous`, `durationDays`, `generateAlarms` e `clientId`. O endpoint de confirmação deve devolver os medicamentos efetivamente criados em `medications` (ou `createdMedications`), preservando `clientId` e incluindo o ID canônico e os mesmos campos de posologia. O frontend usa essa resposta para vincular os horários ao registro correto; não interpreta texto livre da receita como instrução médica.

## Relatórios

`POST /reports` cria um relatório e `GET /reports/:id/download` retorna o PDF. O frontend aceita resposta `application/pdf` como `Blob`.

## Configuração de produção

O build de produção exige `VITE_API_URL` configurada no ambiente antes de `npm run build`, por exemplo `https://api.exemplo.com/api`. O padrão `http://localhost:3000/api` é usado somente no modo de desenvolvimento; não deve ser deixado como URL de produção. O servidor precisa implementar as rotas necessárias às funcionalidades ativadas, autenticar cada chamada e validar propriedade/permissão do perfil no servidor (não confiar apenas em `X-Profile-Id`).
