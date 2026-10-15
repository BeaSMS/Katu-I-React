# Katu'I

Aplicativo de saúde com frontend React/Vite, versão web e janela desktop Electron. Este repositório contém o frontend e um serviço complementar de notificações Web Push. **Não contém a API principal de contas, perfis e registros clínicos**: ela precisa ser executada separadamente ou hospedada num servidor.

## Primeiros passos para quem vai contribuir

### 1. Prepare o ambiente

Instale:

- **Node.js 22 LTS** (inclui npm).
- **Git**, se for clonar o projeto e colaborar por branches.
- Para criar instaladores: o sistema operacional de destino; builds macOS/Linux devem ser gerados no sistema correspondente.

Clone o repositório usando o endereço compartilhado pela equipe e abra a pasta do frontend no VS Code. No terminal, confirme que Node e npm estão disponíveis e instale as dependências travadas pelo projeto:

```powershell
node --version
npm --version
npm ci
```

Use `npm ci` quando instalar o projeto do zero ou depois de atualizar a branch; ele instala exatamente as versões do `package-lock.json`.

### 2. Configure a API principal

Este repositório **não inclui a API principal**. Antes de testar login, criar conta ou usar dados sincronizados, peça à equipe o endereço da API de desenvolvimento ou acesso ao repositório separado e siga as instruções dele para iniciá-la. O padrão esperado pelo frontend é `http://localhost:3000/api`; não há comando neste projeto que inicie esse backend.

Crie o arquivo `.env` local a partir do exemplo. No PowerShell:

```powershell
Copy-Item .env.example .env
```

No macOS/Linux:

```sh
cp .env.example .env
```

Confira `VITE_API_URL` no `.env` e ajuste se a equipe fornecer outra URL. Depois de iniciar a API principal, a verificação de saúde deve responder em `http://localhost:3000/api/health` (ou na URL configurada). Não coloque senhas, tokens ou chaves reais no README, no código ou em commits.

### 3. Inicie e experimente o frontend

Para executar no navegador:

```sh
npm run dev
```

Abra o endereço mostrado pelo Vite, normalmente `http://localhost:5173`. Se a API principal ainda não estiver disponível, escolha **Continuar como Guest** para explorar e testar o armazenamento local; login, IA, sincronização e backup na nuvem não funcionarão sem a API.

Para abrir a versão desktop de desenvolvimento, pare o servidor anterior se ele estiver usando a porta `5173` e execute:

```sh
npm run electron:dev
```

Isso inicia o Vite e abre uma janela Electron. A API principal continua sendo um serviço separado e precisa estar acessível para usar uma conta. Push em segundo plano não é suportado no Electron.

### 4. Antes de enviar uma alteração

Crie uma branch para sua tarefa, faça as alterações e rode as verificações:

```sh
git switch -c feat/descricao-curta
npm test
npm run build
```

Se alterou o serviço de Push, confira também as configurações em `.env` e teste com a API principal ativa. Ao concluir, revise `git diff`, faça commit na sua branch e abra um pull request conforme o fluxo combinado pela equipe. Não inclua `.env`, `node_modules/`, dados locais de Push ou instaladores gerados.

### Onde procurar

- `src/`: interface React e serviços do frontend.
- `src/services/api.js`: chamadas à API principal e fila offline.
- `src/services/resources.js`: catálogo de recursos/telas.
- `electron/`: processo principal e configuração da janela Electron.
- `server/push-server.js`: backend complementar de Web Push; não é a API principal.
- `docs/api-contract.md`: contrato e rotas esperadas da API principal.
- `docs/push-service.md`: configuração e rotas do serviço Push.
- `docs/project-next-steps-and-screens.md`: prioridades sugeridas para o projeto e inventário das telas/dados esperados.
- `tests/`: testes automatizados do projeto.

## Arquitetura e backends

O sistema é composto por partes diferentes:

| Componente | Onde roda | Responsabilidade | Quando é necessário |
|---|---|---|---|
| Frontend React/Vite ou desktop Electron | Computador da pessoa usuária | Interface, modo Guest, armazenamento local e fila offline | Para abrir e usar a interface |
| API principal do Katu'I | Backend local durante desenvolvimento ou servidor hospedado | Cadastro/login, sessão, perfis, dados de saúde, IA e backup | Para todas as funções de conta e sincronização |
| Serviço complementar Web Push (`server/push-server.js`) | Processo Node separado, local ou hospedado | Inscrições e agendamento de notificações Web Push | Para Push em segundo plano na versão web |

Para usar uma conta, o frontend **sempre precisa alcançar a API principal**. Durante desenvolvimento, a URL padrão é `http://localhost:3000/api`, mas isso só funciona se a API principal estiver rodando separadamente nessa máquina/porta. Este repositório não possui código nem comando para iniciar essa API. Em produção, configure `VITE_API_URL` com a URL HTTPS da API hospedada.

O serviço Push deste repositório é um segundo backend; não substitui a API principal. Por padrão, ele escuta na porta `3001` e consulta `${AUTH_API_URL}/me` na API principal para validar tokens e perfis. Ele é opcional para a versão web sem Push. O Electron não usa Web Push; oferece apenas notificações locais enquanto o app está aberto.

**Para testar o fluxo completo de conta e alarmes Web Push em desenvolvimento, os dois backends precisam estar ativos ao mesmo tempo:** a API principal local (padrão porta `3000`) e o serviço Push local (padrão porta `3001`), além do frontend. Sem Push, login e recursos de conta ainda podem funcionar pela API principal, mas alarmes Web Push não estarão disponíveis. Em produção, a API principal e o serviço Push também precisam estar implantados e acessíveis para habilitar esse fluxo.

### Cenários

**Desenvolvimento web com conta**

1. Inicie a API principal separadamente, por padrão em `http://localhost:3000/api`.
2. Execute `npm install` e `npm run dev`.
3. Abra `http://localhost:5173`.

**Desenvolvimento Electron com conta**

1. Inicie a API principal separadamente e configure `VITE_API_URL` para uma URL alcançável pelo desktop.
2. Execute `npm run electron:dev`. O script inicia o Vite e abre uma janela própria.
3. Login, perfis e dados sincronizados continuam dependendo da API principal.

**Modo Guest**

Não exige a API principal nem o serviço Push para registros locais. Guest não tem IA, sincronização ou backup na nuvem.

**Web Push em desenvolvimento**

Inicie a API principal, configure `.env` com `VITE_API_URL`, `AUTH_API_URL`, `VITE_PUSH_API_URL`, chaves VAPID e origens web permitidas; então inicie `npm run server:push` e `npm run dev`. Em produção, a API principal e o serviço Push precisam estar implantados e acessíveis pelo frontend; o serviço Push precisa permanecer ativo para despachar alarmes.

> A janela empacotada do Electron carrega o frontend por `file://`, uma origem diferente de `http://localhost` ou de um domínio web. A API principal precisa ser acessível do desktop e a política CORS/autenticação precisa ser compatível com essa origem. Não permita globalmente a origem `null` como atalho. Teste o login e as chamadas à API num build desktop real antes de distribuir.

## Rotas necessárias

O frontend chama a API principal usando `VITE_API_URL` como base (incluindo `/api`, por exemplo) e acrescenta os caminhos abaixo. As rotas de conta e dados usam `Authorization: Bearer <token>`; operações por perfil também usam `X-Profile-Id` quando aplicável.

| API principal | Rotas usadas/esperadas |
|---|---|
| Sessão e inicialização | `GET /health`, `GET /overview`, `POST /auth/register`, `POST /auth/login`, `GET /me` |
| Perfis | `POST /profiles/me`, `GET/POST /profiles`, `PATCH/DELETE /profiles/:id`, `PATCH /profiles/:id/ai-settings` |
| Dados de saúde | CRUD (`GET`, `POST`, `PATCH /:id`, `DELETE /:id`) em `/medications`, `/alarms`, `/prescriptions`, `/vital-signs`, `/symptoms`, `/feminine-health`, `/pregnancy`, `/appointments`, `/vaccines`, `/exams`, `/documents` e `/reports` |
| Recursos especiais | `POST /ai/prescription`, `POST /prescriptions/:id/confirm`, `GET /reports/:id/download`, `POST /backup` |

Nem toda função acessa todas as rotas: `/health` e `/me` são usados para verificar a API/sessão; rotas de registros são chamadas conforme a tela e as ações da pessoa; IA, relatórios, backup e Push só são usados ao solicitar esses recursos.

O backend complementar Push expõe `GET /vapid-public-key`, `POST/DELETE /subscriptions`, `POST /medication-schedules`, `DELETE /medication-schedules/:medicationId`, `GET/POST /alarms` e `DELETE /alarms/:sourceId`. Rotas Push, autenticação e configuração estão em [`docs/push-service.md`](./docs/push-service.md). O contrato completo da API principal está em [`docs/api-contract.md`](./docs/api-contract.md).

## Armazenamento e funcionamento

- Dados Guest, perfis locais, rascunhos e fila offline ficam em IndexedDB com AES-GCM e chave não-extraível no mesmo perfil local. Dados locais do Electron são separados dos dados do navegador e de outros usuários do sistema.
- A criptografia reduz exposição por leitura casual, mas não protege contra XSS/código malicioso na origem nem contra alguém com acesso ao perfil completo do sistema ou do navegador.
- Dados Guest não são enviados aos servidores e não têm backup automático. Exporte backups regularmente; limpar os dados locais, trocar de computador ou conta do sistema pode torná-los inacessíveis.
- A fila offline guarda ações comuns quando há falha de rede/timeout e as reenvia quando a conexão retorna. Offline não significa que a API principal esteja empacotada ou rodando localmente. Algumas operações, como IA e sincronização, exigem conexão.
- Notificações locais só podem ser verificadas enquanto a interface está aberta. Web Push pode funcionar com a página fechada, dependendo do serviço Push, navegador, rede e sistema operacional; não é oferecido no Electron.
- Após criar ou remover registros concluídos, use **Desfazer** ou `Ctrl+Z` (`Command+Z` no macOS). Ações ainda pendentes na fila offline não podem ser desfeitas.

## Executar e empacotar

```bash
npm install
npm run dev                 # frontend web: http://localhost:5173
npm run electron:dev         # Vite + janela Electron
npm test                     # testes automatizados
npm run build                # build web
npm run preview              # servir o build web
```

Configure `VITE_API_URL` antes de gerar builds de produção; variáveis `VITE_*` são incluídas no bundle durante o build.

```bash
npm run build:desktop        # Windows x64, instalador NSIS
npm run build:desktop:mac    # macOS, DMG
npm run build:desktop:linux  # Linux, AppImage
```

Os instaladores ficam em `release/`. Gere cada pacote no sistema operacional correspondente. Electron inclui Chromium próprio; não depende do navegador instalado.

## Serviço Web Push (opcional para web)

1. Copie `.env.example` para `.env` e configure URLs da API principal e do Push.
2. Gere chaves com `npx web-push generate-vapid-keys`; configure `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` e `VAPID_SUBJECT`. Nunca publique a chave privada.
3. Com a API principal já ativa, inicie `npm run server:push` e `npm run dev`.
4. Na interface web, abra **Configurações > Preferências gerais > Alarmes em segundo plano** e permita notificações.

O serviço armazena agendamentos em `server/data/push-store.json`. Em produção, use volume persistente privado, HTTPS e `CORS_ALLOWED_ORIGINS` restrito às origens exatas da versão web. É uma implementação de instância única; não compartilhe o JSON entre réplicas concorrentes.

O Guest nunca chama o serviço Push. Horários e mensagens de notificação são genéricos para não expor dados de saúde na tela bloqueada. A entrega não é garantida em horário exato e pode ser atrasada pelo navegador ou sistema operacional.
