# Telas e formulários do Katu'I

Este documento descreve as telas e os fluxos de entrada de dados identificados no código do projeto. O repositório contém duas interfaces: a aplicação web em React e o aplicativo Android nativo em Jetpack Compose. Os nomes abaixo descrevem a função do fluxo; campos opcionais podem variar conforme o tipo de registro, o perfil ativo, a conexão e os recursos habilitados.

## Cores das telas e formulários

As cores abaixo foram identificadas nos estilos e temas da aplicação. As cores de acento identificam categorias e ações; não significam que toda a tela ou formulário use uma cor sólida dessa categoria.

### Paleta base

| Interface/estado | Cores | Aplicação |
|---|---|---|
| Web, tema claro | Fundo geral `#F6F8F6` / `#F7F9F7`, painéis `#FFFFFF`, texto principal `#213836`, verde-petróleo de marca e ações `#176B62` | Fundo e texto base do app, painéis e elementos de marca/ação. |
| Web, navegação | Inativo: texto `#6D827D`; hover: fundo `#F2F7F5` e texto `#176B62`; ativo: fundo `#E8F4F0` e texto `#176B62`; indicador: fundo `#D4EDE6` | Estado visual do item selecionado, hover e contadores do menu lateral. |
| Web, tema escuro | Fundo `#172522`, superfícies `#21332F`, texto `#DBE9E4`; links/realces `#8DCDBB` | Substitui fundos e textos base; ajustes específicos podem variar por componente. |
| Android, tema claro | Fundo `#F8FBFB`, cartões `#FFFFFF`, texto `#191C1C`, primária `#009688`, secundária `#00B0FF`, erro `#BA1A1A` | Paleta Material usada pelas telas e componentes que não têm cor própria de categoria. |
| Android, tema escuro | Fundo/superfície `#191C1C`, texto `#E0E3E2`, primária `#80D3C9`, secundária `#B1CCC8`, erro `#FFB4AB` | Cores base do esquema escuro; acentos das categorias de saúde permanecem próprios. |
| Android, tema por perfil | A cor-semente do perfil gera primária e cores relacionadas; fundos e superfícies permanecem os do tema | Quando há cor personalizada, os controles Material usam variações calculadas a partir dela. |
| Web, cor do perfil | Turquesa `#009C9C`, dourado `#D89A27`, azul `#2F8FB0`, violeta `#7054A5`, rosa `#C66B88` ou verde `#4A9A73` | Opções do seletor de cor do perfil; cada opção também possui variantes suaves e escuras. Não substitui a cor base de todos os componentes. |

### Aplicação web — telas e formulários

As telas e formulários web herdam a paleta base e o tema claro/escuro. A coluna de acento registra apenas uma cor de categoria comprovada nos atalhos de adição rápida; nos demais casos, o código não define uma cor exclusiva para a tela.

| Tela ou fluxo | Cores usadas / comportamento |
|---|---|
| Início, Entrar e Cadastro | Paleta base web; superfícies claras ou escuras conforme o tema. Sem acento de categoria específico identificado. |
| Integração inicial | Paleta base web. A escolha de cor do perfil mostra as seis opções de perfil indicadas na paleta; etapa de IA e campos não têm uma cor exclusiva identificada. |
| Visão geral | Paleta base web e cor do perfil para elementos configurados para refletir o perfil; sem cor única para toda a tela. |
| Medicamentos e formulário de medicamento | Paleta base web; atalho de Remédio usa ícone verde-azulado (`#408D7D`) em fundo suave (`#E2F2EE`). |
| Alarmes e formulário de alarme | Paleta base web; sem acento exclusivo de formulário identificado. |
| Receitas e formulários de receita/revisão | Paleta base web; atalho de Receita usa dourado (`#AF8240`) em fundo suave (`#F9F0DC`). |
| Diário de sintomas, formulário físico e formulário de humor | Paleta base web; atalho Sintoma usa violeta (`#8D80B4`) em fundo suave (`#EFEBFA`). |
| Sinais vitais e formulário de medição | Paleta base web; atalho usa rosa (`#BC6680`) em fundo suave (`#FAE8EE`). |
| Consultas e formulário de consulta | Paleta base web; atalho usa azul (`#668EB6`) em fundo suave (`#E8F0F8`). |
| Exames e formulário de exame | Paleta base web; atalho usa rosa (`#B55D87`) em fundo suave (`#F9E7F1`). |
| Documentos e formulário genérico de registro | Paleta base web; sem acento exclusivo identificado. |
| Saúde feminina e seus formulários | Paleta base web; atalho usa magenta-rosa (`#B75D8C`) em fundo suave (`#FAE5F1`). |
| Carteira de vacinação e formulário de vacina | Paleta base web; atalho usa verde (`#4C977F`) em fundo suave (`#E4F3ED`). |
| Perfis/dependentes e formulário de dependente | Paleta base web e cor configurada para cada perfil quando exibida; sem acento exclusivo para o formulário. |
| Backup pessoal, Configurações e Ajuda | Paleta base web; o tema escuro altera superfícies/textos. A escolha de cor do perfil apresenta as opções da paleta base de perfil. |
| Ações rápidas e submenus | Os acentos por tipo de registro são os listados acima. Os submenus de Sinais vitais e Saúde feminina não têm cores/ícones individuais identificados nos itens. |

## Aplicação web

### Acesso e configuração inicial

| Tela | Descrição |
|---|---|
| Início (`/`) | Apresenta o Katu'I e oferece entrada na conta, criação de conta ou uso como Guest. O modo Guest mantém os registros neste dispositivo e não oferece IA nem backup em nuvem. |
| Entrar (`/entrar`) | Formulário de acesso com e-mail e senha. Pode oferecer a importação dos registros Guest para a conta, mediante escolha explícita. |
| Cadastro (`/cadastro`) | Cria uma conta com nome completo, e-mail, senha e confirmação de senha. |
| Integração inicial (`/integracao-inicial`) | Assistente em etapas para preparar o perfil. Pode ser pulado em uma conta. No modo Guest, a IA não é oferecida. |
| Visão geral (`/aplicativo/visao-geral`) | Painel com calendário, contagem de registros, próximos medicamentos, consulta do dia e resumo dos sintomas. Atalhos levam aos módulos e ao registro de um sintoma. |

#### Formulário de integração inicial

As etapas são: boas-vindas; nome; idade; sexo biológico; peso; altura; alergias; condições especiais; escolha opcional de IA; e revisão dos dados. Nome, idade, peso e altura têm validações de faixa. A tela final resume nome, idade, sexo e preferência de IA. Os campos de alergias e condições aceitam texto livre.

### Telas de cuidado e registros

Os módulos abaixo usam calendário e lista de registros por data quando aplicável. A ação principal abre o formulário correspondente; registros existentes podem ser removidos após confirmação.

| Tela | Descrição |
|---|---|
| Medicamentos (`/aplicativo/medicamentos`) | Organiza tratamentos, posologia, frequência, duração, estoque e lembretes. |
| Alarmes (`/aplicativo/alarmes`) | Lista lembretes e permite criar alarmes associados ou não a um medicamento. |
| Receitas (`/aplicativo/receitas`) | Lista receitas e seus anexos. Permite cadastrar a receita com foto ou PDF e, depois de salvar, solicitar a leitura por IA para revisar medicamentos sugeridos e configurar alarmes antes da confirmação. Guest pode cadastrar receitas e manter anexos localmente, mas não usa IA. |
| Diário de sintomas (`/aplicativo/sintomas`) | Exibe registros de sintomas físicos e de humor; permite registrar ambos os tipos. |
| Sinais vitais (`/aplicativo/sinais-vitais`) | Registra e acompanha pressão, glicose, batimentos, saturação, temperatura, peso e altura. |
| Consultas (`/aplicativo/consultas`) | Organiza compromissos de saúde, com profissional, especialidade, local e horário. |
| Exames (`/aplicativo/exames`) | Guarda resultados de exames, categoria, tipo de resultado, data e observações, com o arquivo do resultado anexado. |
| Documentos (`/aplicativo/documentos`) | Lista documentos de saúde; o cadastro usa título, detalhes, data e observações, e permite anexar o arquivo correspondente. |
| Saúde feminina (`/aplicativo/saude-feminina`) | Mostra calendário e registros de ciclo, sintomas, humor e anticoncepcional. Também permite iniciar ou encerrar acompanhamento de gestação. |
| Carteira de vacinação (`/aplicativo/vacinas`) | Lista vacinas e permite registrar doses, inclusive quando a data exata não é conhecida. |
| Perfis e dependentes (`/aplicativo/perfis`) | Gerencia perfis de pessoas sob cuidado e permite adicionar dependentes com informações de saúde. Essa área não é exibida no modo Guest. |
| Backup pessoal (`/aplicativo/backup-pessoal`) | Área de registros do módulo e ação de backup. As operações de exportação e importação também estão em Configurações. Essa área não é exibida no modo Guest. |
| Configurações (`/aplicativo/configuracoes`) | Reúne preferências gerais, IA, acessibilidade, privacidade e backup. Algumas opções dependem de conta, plataforma ou serviço configurado. |
| Ajuda (`/aplicativo/ajuda`) | Central de perguntas frequentes com busca por texto e respostas expansíveis. |

### Formulários, diálogos e ações web

#### Formulário de medicamento

- **Medicamento comum:** nome (obrigatório), dosagem, finalidade, data e horário iniciais, frequência (intervalo de horas ou vezes ao dia), quantidade da frequência, uso contínuo ou duração, quantidade em estoque, estoque mínimo, doses por tomada, unidade de medida e opção de gerar alarmes.
- **Anticoncepcional:** nome, padrão da cartela, data de início, horário diário e opção de lembrete diário.

#### Formulário de alarme

Título ou descrição (obrigatório), medicamento vinculado (opcional), data, horário e observações. O horário precisa ser futuro.

#### Formulários de receita

- **Cadastro:** título (obrigatório), data da receita, observações e anexo opcional em PDF, JPG ou PNG de até 15 MB.
- **Leitura por IA:** pode ser iniciada após salvar uma receita com anexo; usa o documento salvo sem exigir novo envio. Também pode começar pelo envio direto de um arquivo.
- **Revisão da leitura:** permite selecionar os medicamentos reconhecidos e, quando os alarmes estiverem disponíveis, configurar data, horário, frequência, uso contínuo ou duração antes de confirmar. Só os medicamentos selecionados são cadastrados; a confirmação fica vinculada à receita original.
- **Revisão da leitura:** permite selecionar os medicamentos reconhecidos e, quando os alarmes estiverem disponíveis, configurar data, horário, frequência, uso contínuo ou duração antes de confirmar.

#### Formulário de sinais vitais

Escolha do tipo de registro e valor específico: pressão sistólica e diastólica; glicose; batimentos; saturação; temperatura; peso; ou altura. Inclui data, hora e observações. A data/hora não pode estar no futuro.

#### Formulário de consulta

Nome do profissional (obrigatório), especialidade, local ou clínica, data, hora e observações.

#### Formulário de exame

Nome e arquivo do resultado (obrigatórios), categoria (laboratório, imagem, cardiologia, endoscopia ou outros), tipo de resultado (imagem única, série temporal ou resultado numérico), data e observações. O arquivo pode ser PDF, JPG ou PNG de até 15 MB.

#### Formulário de documento

Título (obrigatório), detalhes, data e observações; permite anexar opcionalmente um PDF, JPG ou PNG de até 15 MB.

#### Formulário de sintoma e humor

- **Físico:** descrição do que está sentindo, localização escolhida ou texto livre, duração, fatores que pioram ou melhoram, impacto no dia, indicação de que impede atividades e intensidade de 1 a 10.
- **Humor:** clima geral (positivo, neutro, baixo ou tenso), nuances relacionadas ao clima, gatilhos ou motivos e intensidade de 1 a 10.
- Ambos permitem informar a data do registro. A descrição do sintoma físico ou o clima do humor é obrigatório.

#### Formulários de saúde feminina

- **Início do ciclo:** data de início, intensidade do fluxo e observações ou sintomas.
- **Fim do ciclo:** último dia do fluxo, que não pode anteceder o início nem ser futuro.
- **Registro diário:** data, fluxo, sintomas físicos, humor e observações.
- **Tomada de anticoncepcional:** data, horário, confirmação de dose tomada e observações.
- **Acompanhamento de gestação:** data de início usada no acompanhamento e confirmação da ativação; a desativação pede confirmação.

#### Formulário de vacina

Nome (obrigatório), data da dose, opção “não sei a data exata” e observações, como lote ou local.

#### Formulário de dependente

Assistente em etapas com nome, vínculo, idade, sexo biológico, peso, altura, alergias, condições especiais e revisão final.

#### Configurações e outros diálogos

- **Preferências gerais:** alterna notificações, resumo semanal, tema escuro, feedback sonoro, cor do perfil e lembretes locais ou em segundo plano, conforme a plataforma e a disponibilidade.
- **Inteligência artificial:** escolhe IA padrão ou provedor (OpenAI, Google Gemini ou Anthropic), modelo opcional e chave de API. Permite salvar ou remover a configuração.
- **Acessibilidade:** escala de texto de 100% a 200%, fonte para dislexia, alto contraste e redução de animações.
- **Privacidade e segurança:** exibe o estado da sessão e da sincronização; permite apagar os dados locais mediante a ação correspondente.
- **Backup:** exporta dados ou importa um arquivo JSON; também oferece sincronização do backup quando possível.
- **Cor do perfil:** diálogo de escolha de tema para o perfil ativo.
- **Ações rápidas:** menu de atalhos para criar registros; encaminha para os mesmos formulários dos módulos.
- **Confirmação de remoção:** informa qual registro será removido e pede confirmação antes da exclusão.
- **Busca da Ajuda:** campo de texto que filtra as perguntas frequentes.

## Aplicativo Android nativo

O fluxo inicial do Android não usa as telas de login e cadastro da web. Na primeira execução, apresenta permissões e depois cria o perfil local. Após essa configuração, as telas principais são acessadas pelo menu inferior expansível.

### Telas

| Tela | Descrição |
|---|---|
| Permissões | Explica e solicita permissões necessárias antes de continuar a configuração inicial. |
| Perfil inicial | Cadastro inicial dos dados de saúde do usuário; conclui o onboarding e abre a aplicação. |
| Home / Dashboard | Resumo principal e atalhos para os módulos do app. |
| Remédios | Lista e gerencia medicamentos do perfil. Tem acesso à importação de receita. |
| Alarmes | Consulta e gerencia lembretes de medicamentos e outros cuidados. |
| Sintomas | Registra e consulta sintomas físicos e humor, com calendário e histórico. |
| Sinais Vitais | Registra e acompanha medições de saúde. |
| Consultas | Agenda e consulta compromissos de saúde. |
| Exames | Registra exames e acessa seus resultados. |
| Receitas | Consulta prescrições cadastradas e inicia importação de receita. |
| Importar Receita | Fluxo para selecionar e processar uma receita; ao concluir, retorna à tela de medicamentos. |
| Vacinas | Consulta e registra vacinações. |
| Saúde Feminina | Acompanhamento de ciclo e cuidados femininos; disponível para perfis elegíveis pelo sexo biológico informado. |
| Minha Gestação | Acompanhamento da gestação, com acesso a sintomas e consultas; também depende de perfil elegível. |
| Perfil | Exibe e gerencia informações do perfil ativo e oferece acesso à tela Sobre. |
| Configurações | Preferências do aplicativo, incluindo aparência, acessibilidade e opções de conta. |
| Conta Katu'I | Gerencia a conta remota e sua integração com o perfil local. |
| Sobre o App | Informações sobre o aplicativo. |
| Alarme em tela cheia | Tela/atividade aberta pelo fluxo de alarme para apresentar e tratar o lembrete ativo. |

### Formulários e fluxos de entrada Android

| Formulário ou fluxo | Dados e finalidade |
|---|---|
| Perfil inicial | Reúne as informações pessoais e de saúde usadas para criar o perfil local. |
| Medicamento | Cadastro e edição de tratamento e de seus dados de uso; a tela também dá acesso à importação de receita. |
| Alarme | Configuração e tratamento de lembretes, incluindo ações disponíveis quando o alarme é exibido. |
| Sintoma | Registro de sintomas e humor associado à data selecionada. |
| Sinal vital | Seleção do indicador e registro de sua medição. |
| Consulta | Cadastro de compromisso de saúde. |
| Exame | Cadastro e organização de exames e resultados. |
| Receita | Cadastro e consulta de prescrições. |
| Importação de receita | Seleção de uma receita para importação e revisão/processamento antes de concluir. |
| Vacina | Cadastro de uma dose e seus dados de aplicação. |
| Saúde feminina | Registro e consulta de dados de ciclo e cuidados relacionados. |
| Gestação | Registro e acompanhamento das informações da gestação. |
| Perfil | Edição ou gerenciamento das informações do usuário ativo, conforme as opções exibidas na tela. |
| Configurações | Controles de preferências; inclui escolhas de aparência, acessibilidade e som de alarme, em vez de um formulário único de cadastro. |
| Conta Katu'I | Formulários e ações para configurar ou gerenciar a conta remota. |

### Cores das telas e formulários Android

O fundo, cartões e textos usam o esquema claro/escuro descrito na paleta base, exceto quando há cor de categoria própria. Nos cartões do menu de saúde, o acento colore o ícone, o fundo e a borda com transparências; o nome usa a cor de texto do tema.

| Tela ou formulário | Acento de categoria e aplicação |
|---|---|
| Permissões, Perfil inicial, Home/Dashboard, Perfil e Sobre o App | Cores Material do tema. A primária padrão é `#009688`; uma cor-semente de perfil pode alterar a paleta de componentes Material. |
| Remédios e formulário de medicamento | Teal `#009688`; identifica o módulo nos cartões/ícones de saúde. |
| Alarmes e formulário de alarme | Cores Material do tema; o atalho circular usa a cor secundária do tema (padrão `#00B0FF`). |
| Sintomas e formulário de sintoma/humor | Roxo `#673AB7`. |
| Sinais Vitais e formulário de medição | Vermelho `#F44336`. |
| Consultas e formulário de consulta | Azul-ciano `#00B0FF`. |
| Exames e formulário de exame | Rosa `#E91E63`. |
| Receitas, formulário e importação de receita | Laranja `#FF9800`. |
| Vacinas e formulário de vacina | Verde `#4CAF50`. |
| Saúde Feminina e formulário relacionado | Magenta `#FF4081`; a disponibilidade depende do perfil elegível. |
| Minha Gestação e fluxo de gestação | Cores Material do tema; não foi identificada uma cor exclusiva para esse fluxo. |
| Configurações e Conta Katu'I | Cores Material do tema; controles de aparência podem alterar tema e cor-semente do perfil. |
| Seletor de perfil do menu | Cor do usuário ativo no avatar/cartão. Perfil inativo usa cor neutra do tema; no escuro, o fundo/borda recebem transparências ajustadas para contraste. |

> **Nota sobre formulários Android:** os formulários nativos são implementados nas telas Compose e podem usar seletores e diálogos do sistema. Esta lista descreve a finalidade de cada fluxo, sem fixar campos que podem depender da implementação ou do tipo de registro exibido.

## Observações de escopo

- A web oferece login/cadastro, onboarding em várias etapas e modo Guest; o Android inicia com permissões e onboarding de perfil local.
- A leitura de receitas por IA está documentada na interface web; a tela Android identificada é a de importação de receita e seu fluxo de processamento.
- Alguns módulos de registro usam um formulário compartilhado com campos que mudam conforme a ação escolhida.
- As telas e descrições refletem a navegação e os componentes encontrados no código, não substituindo instruções clínicas ou orientações de uso.
