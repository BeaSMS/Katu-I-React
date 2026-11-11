# Serviço de alarmes Web Push

O serviço em `server/push-server.js` é um backend complementar, separado da interface Vite e da **API principal**. Ele é necessário para Web Push em segundo plano na versão web, mas não substitui a API principal de contas e dados. Durante desenvolvimento, inicie também a API principal (padrão `http://localhost:3000/api`); o serviço Push consulta `/me` nessa API para validar cada token e perfil. Em produção, ambos os serviços precisam estar implantados e acessíveis.

O processo Push precisa continuar ativo para verificar e enviar os alarmes. É um serviço de uma instância: seu armazenamento JSON local não coordena múltiplas réplicas.

## Variáveis de ambiente

Configure-as no ambiente do processo do servidor, sem publicar o `.env`:

| Variável | Uso |
|---|---|
| `AUTH_API_URL` | Base da API principal; o serviço consulta `${AUTH_API_URL}/me`. |
| `PUSH_PORT` | Porta HTTP do serviço (padrão `3001`). |
| `CORS_ALLOWED_ORIGINS` | Origens web exatas da interface, separadas por vírgula. |
| `VAPID_PUBLIC_KEY` | Chave pública retornada ao navegador. |
| `VAPID_PRIVATE_KEY` | Chave privada usada apenas pelo servidor. |
| `VAPID_SUBJECT` | Contato VAPID, normalmente `mailto:...` ou URL. |
| `PUSH_STORE_PATH` | Caminho opcional para o JSON persistente; padrão `server/data/push-store.json`. |
| `VITE_PUSH_API_URL` | URL base do Push incorporada ao build da interface web; por padrão local, `http://localhost:3001`. |

Para desenvolvimento web, o exemplo assume API principal em `http://localhost:3000/api`, interface em `http://localhost:5173` e serviço Push em `http://localhost:3001`. Gere um par de chaves com `npx web-push generate-vapid-keys`. Use HTTPS na interface e no serviço em produção; `localhost` é permitido para desenvolvimento. Mantenha a chave privada e os dados em volume privado. A escrita atômica reduz corrupção em reinicializações, mas não substitui banco transacional, controle de acesso ao volume nem coordenação de múltiplas instâncias.

Em produção, configure `VITE_PUSH_API_URL` com a URL pública do serviço Push antes do build e `AUTH_API_URL` com a base da API principal implantada. O frontend e ambos os serviços precisam conseguir se comunicar. Restrinja `CORS_ALLOWED_ORIGINS` às origens web exatas; não inclua origens amplas como `*`.

## Rotas HTTP

| Método | Rota | Autenticação | Uso |
|---|---|---|---|
| `GET` | `/vapid-public-key` | Pública | Obtém a chave pública para inscrição do navegador. |
| `POST` | `/subscriptions` | Bearer + perfil | Registra ou atualiza uma assinatura Web Push. |
| `DELETE` | `/subscriptions` | Bearer + perfil | Remove a assinatura indicada por `{ "endpoint": "https://..." }`. |
| `POST` | `/medication-schedules` | Bearer + perfil | Cria/substitui horários recorrentes de medicamento. |
| `DELETE` | `/medication-schedules/:medicationId` | Bearer + perfil | Remove os horários recorrentes do medicamento. |
| `GET` | `/alarms` | Bearer + perfil | Lista os alarmes Push daquele perfil. |
| `POST` | `/alarms` | Bearer + perfil | Cria ou substitui um lembrete/alarmes restaurado. |
| `DELETE` | `/alarms/:sourceId` | Bearer + perfil | Remove o lembrete daquele perfil. |

Todas as rotas, exceto `GET /vapid-public-key`, exigem `Authorization: Bearer <token>` e `X-Profile-Id`. O serviço chama `${AUTH_API_URL}/me`, encaminhando ambos os headers. A resposta de `/me` precisa conter `profiles` com o perfil solicitado e um identificador estável da conta (`user.id`, `userId` ou `id`). O serviço confirma que o perfil pertence à conta antes de consultar ou alterar dados.

`POST /medication-schedules` recebe os campos de agenda, incluindo `medicationId`, `medicationName`, `startAt` (ISO), `localDate`, `localTime`, `timeZone`, `intervalMinutes` e `durationDays` (nulo para uso contínuo). `POST /alarms` recebe `sourceId`, `startAt`, fuso/data/hora local, e opcionalmente `intervalMinutes`, `durationDays`, `oneTime` e texto de exibição genérico. Alarmes pontuais precisam estar no futuro.

O navegador envia sua origem e o serviço só permite origens presentes em `CORS_ALLOWED_ORIGINS`. A chave VAPID privada e o arquivo JSON de agendamentos devem permanecer apenas no servidor.

## Agendamento e entrega

O intervalo de `N` horas gera um horário a cada `N * 60` minutos. “N vezes ao dia” é distribuído uniformemente em um período de 24 horas. O primeiro horário é convertido pelo navegador para ISO; repetições subsequentes usam intervalos de tempo decorrido em minutos. A duração é contada desde esse primeiro horário; alarmes atrasados não são enviados em rajada, e a rotina avança para o próximo horário futuro. Registros com início anterior podem ser sincronizados se ainda houver um horário futuro dentro da duração.

As notificações não incluem nome, dose ou outros dados de saúde na tela bloqueada. A entrega depende da permissão do navegador, dispositivo conectado, sistema operacional e serviços Push do navegador; não há garantia de horário exato. A remoção de uma assinatura não apaga os agendamentos, permitindo que o app os sincronize novamente quando as notificações forem reativadas.

O Guest nunca chama este serviço. Notificações Guest são locais e funcionam somente enquanto a página está aberta; não devem ser descritas como alarmes em segundo plano.
