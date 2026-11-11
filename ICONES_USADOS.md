# Ícones usados no Katu'I

Inventário dos ícones encontrados no código da aplicação web e do aplicativo Android. A interface web desenha os SVGs com o componente `Icone`; o Android usa principalmente os vetores Material Icons do Jetpack Compose. Os recursos rasterizados e XML específicos da marca também estão listados. Os nomes de arquivo abaixo são os nomes descritivos da cópia em `Icons/`; as referências Android da aplicação continuam com os nomes originais em `Katu'I Offline/app/src/main/res`.

## Web — SVGs da interface

Os 34 SVGs desta tabela estão em `Icons/frontend/svg/`. “Partes” descreve as telas, componentes ou ações em que aparecem; ícones usados como valor em listas aparecem através do componente compartilhado.

| Arquivo | O que representa | Partes do app |
|---|---|---|
| `add.svg` | Adicionar ou criar um registro. | Ações de adicionar, estados vazios e formulários dos módulos; cadastro de dependente e registro rápido. |
| `arrow-right.svg` | Avançar, abrir ou sair; a direção depende do texto ao lado. | Navegação e ações da barra lateral, botões de formulário e integração, atalhos, detalhes de alarme, ajuda e ações de backup. |
| `blood-pressure.svg` | Coração/saúde cardiovascular. | Opção Pressão arterial no formulário de sinais vitais; também identifica humor e cuidado com perfis em telas compartilhadas. |
| `blood.svg` | Gota, usada como representação visual de glicose. | Opção Glicose no formulário de sinais vitais. |
| `brain.svg` | Saúde mental e pensamento. | Formulário de sintoma/humor, seleção de nuances e classificação de humor tenso. |
| `calendar.svg` | Calendário e data. | Calendário de registros, consultas, visão geral e módulos de calendário. |
| `checkmark.svg` | Seleção, conclusão ou confirmação. | Perfil ativo, sucesso após uma ação, opções escolhidas e destaque de benefícios na tela de entrada. |
| `chevron-right.svg` | Abrir detalhes ou avançar em uma lista. | Navegação do calendário, menus expansíveis, perguntas da Ajuda e cartões da visão geral. |
| `cloudy-weather.svg` | Nuvem/clima nublado, associado a estado emocional ou conectividade. | Seleção de clima no formulário de humor e informação de funcionamento offline na tela inicial. |
| `dashboard.svg` | Visão geral e grade de navegação. | Item Visão geral da navegação web e botão do menu móvel. |
| `document.svg` | Arquivo ou documento. | Exames, receitas e documentos; importação de backup e criação de relatório PDF. |
| `folder.svg` | Pasta e arquivos organizados. | Navegação para Documentos. |
| `health-journal.svg` | Diário e histórico de sintomas. | Navegação para Diário de sintomas. |
| `heart-rate.svg` | Pulso e monitoramento de saúde. | Navegação e cartões de sinais vitais, sintomas físicos, batimentos cardíacos e bem-estar. |
| `height.svg` | Medição de altura. | Opção Altura no formulário de sinais vitais. |
| `help.svg` | Ajuda e suporte. | Acesso à Central de Ajuda na navegação principal. |
| `insights.svg` | Destaque, sugestão ou informação inteligente. | Leitura de receita com IA, resumos e nuances de humor. |
| `lightning.svg` | Energia ou atividade. | Nuances de humor, como motivação e energia. |
| `location-pin.svg` | Localização. | Seleção ou preenchimento da localização de um sintoma físico. |
| `medication.svg` | Comprimido e tratamento. | Navegação de Medicamentos, cartões e próximos medicamentos, saúde feminina e ações de tratamento. |
| `notifications.svg` | Notificação e lembrete. | Indicador de notificações e próximos alarmes; navegação para Alarmes. |
| `oxygen-saturation.svg` | Bateria/nível, usado para representar saturação. | Opção Saturação no formulário de sinais vitais; nuances relacionadas a cansaço ou energia. |
| `preferences.svg` | Ajustes de preferências. | Item de configurações na definição de navegação e controles de preferências. |
| `receipt.svg` | Receita ou prescrição. | Navegação para Receitas. |
| `sad-mood.svg` | Humor triste ou baixo. | Clima e nuances de humor no formulário de sintomas. |
| `search.svg` | Busca e localização de informação. | Busca da Central de Ajuda e fator que influencia um sintoma. |
| `settings.svg` | Configurações. | Acesso à tela Configurações e identificação do módulo. |
| `sunny-weather.svg` | Sol, clima positivo e energia. | Climas e nuances positivas no formulário de humor; seletor de cor de perfil. |
| `temperature.svg` | Temperatura. | Opção Temperatura no formulário de sinais vitais. |
| `time.svg` | Hora ou duração. | Dados de horário no calendário e registros de saúde feminina. |
| `upward-trend.svg` | Tendência ou impacto. | Campo de impacto do sintoma e indicadores de evolução. |
| `user-profile.svg` | Pessoa e perfil. | Perfis e dependentes, autoria/seleção de perfil e opções de humor relacionadas a pessoas. |
| `vaccination.svg` | Vacina e proteção. | Navegação e registros da Carteira de vacinação. |
| `weight.svg` | Medição de peso. | Opção Peso no formulário de sinais vitais. |

### Imagens decorativas e marca da web

| Arquivo | Uso |
|---|---|
| `Icons/frontend/publico/katui-logo.png` | Logotipo exibido pelo componente de marca nas telas web, incluindo acesso, onboarding e navegação. |
| `Icons/frontend/publico/card-background.svg` | Textura/ilustração decorativa usada como fundo de cartões em estilos da interface; não é um controle de ação. |

### Símbolos de texto usados como ícones na web

Alguns atalhos usam caracteres diretamente no JSX, em vez de um SVG. Eles não têm arquivo próprio em `Icons/`.

| Símbolo | O que representa | Partes do app |
|---|---|---|
| `✦` | Destaque, marca ou recurso inteligente. | Início e entrada, onboarding, menu de adição rápida para sintoma/receita, leitura de receita e configurações de IA. |
| `♥` | Cuidado cardiovascular. | Atalho de sinais vitais no menu rápido e atalho para pressão arterial em Sinais Vitais. |
| `♡` | Saúde feminina. | Atalho de Saúde feminina no menu de adição rápida. |
| `✓` | Concluído ou proteção em dia. | Atalho de vacina e confirmação de conclusão no onboarding. |
| `＋` | Adicionar medicamento. | Atalho Remédio no menu de adição rápida. |
| `⌁` | Consulta ou compromisso. | Atalho Consulta no menu de adição rápida. |
| `□` | Exame ou documento. | Atalho Exame no menu de adição rápida. |
| `°` | Graus. | Atalho de temperatura em Sinais Vitais. |
| `◌` | Gota/medição de glicose. | Atalho de glicose em Sinais Vitais. |
| `⚖` | Peso/medição corporal. | Atalho de peso em Sinais Vitais. |
| `♀`, `♂`, `⚥` | Opções de sexo biológico. | Etapa correspondente da integração inicial web. |
| `○` | Não selecionar/adiar uma opção. | Opção “Agora não” da escolha de IA no onboarding. |
| `←` | Voltar. | Botão de retorno das telas de entrada e cadastro. |
| `×` | Fechar ou remover. | Fechamento de diálogos e remoção de etiquetas ou itens em telas web. |
| `!` | Atenção/ação que exige confirmação. | Ícone de diálogo de confirmação no formulário compartilhado. |
| `•••` | Mais opções. | Menu de ações de um registro na lista de recursos. |

## Android — ícones Material do Jetpack Compose

Estes ícones são vetores fornecidos pela biblioteca Material Icons e são referenciados no código Kotlin, não arquivos individuais em `Icons/`. Os nomes dos arquivos indicam os locais de uso dentro de `Katu'I Offline/app/src/main/java/com/katui/offline/`; telas como `DashboardScreen` e `MedicationScreen` são descritas em português na coluna “Partes”.

| Ícone Material | O que representa | Partes do app |
|---|---|---|
| `AccountCircle` | Conta ou identidade da pessoa. | Menu principal e Configurações (`MainActivity`, `SettingsScreen`). |
| `AcUnit` | Frio ou sensação de frio. | Registro de sintomas e sintomas de saúde feminina (`SymptomScreen`, `FemaleHealthScreen`). |
| `Add` | Criar/adicionar. | Ações de adicionar em Alarmes, Consultas, Exames, Saúde feminina, Perfil inicial, Gestação, Receitas, Perfil, Sintomas e Vacinas. |
| `AddCircle` | Adicionar item à lista. | Cadastro de resultado/arquivo em Exames (`ExameScreen`). |
| `Air` | Respiração ou falta de ar. | Categorias de sintomas em Sintomas e Saúde feminina. |
| `Alarm` | Alarme ou lembrete. | Tela de alarmes, notificações/agenda e identificação do módulo (`AlarmActivity`, `AlarmScreen`, `ConsultationScreen`, `MainActivity`, `PermissionsScreen`). |
| `AlarmOff` | Desativar alarme. | Controles da tela Alarmes (`AlarmScreen`). |
| `ArrowBack` | Voltar à tela anterior. | Perfil inicial, gestação, Perfil, Conta Katu'I, Configurações e navegação principal. |
| `ArrowDropDown` | Abrir opções de uma lista. | Seletores em Alarmes, Exames e Importar Receita. |
| `ArrowForward` | Avançar no fluxo. | Etapas do Perfil inicial (`OnboardingProfileScreen`). |
| `Article` | Artigo ou prescrição. | Receitas e identificação da tela Receitas (`PrescriptionScreen`, `MainActivity`). |
| `Assignment` | Registro, tarefa ou ficha de saúde. | Dashboard, categorização de exames, central de saúde e navegação para Sintomas (`DashboardScreen`, `ExamCategorizer`, `HealthHubScreen`, `MainActivity`). |
| `AutoAwesome` | Recurso inteligente ou sugestão. | Ações assistidas no Dashboard, Exames e Saúde feminina. |
| `AutoFixHigh` | Processamento automático/assistido. | Leitura ou tratamento de exames e etapa de IA no Perfil inicial. |
| `AutoGraph` | Gráfico ou análise de dados. | Informações do aplicativo em Sobre (`AboutScreen`). |
| `Badge` | Identificação ou distintivo. | Dados de identificação do Perfil (`ProfileScreen`). |
| `Balance` | Balança ou equilíbrio. | Classificação de exame por categoria (`ExamCategorizer`). |
| `BatteryAlert` | Energia baixa ou fadiga. | Sintomas e registros de Saúde feminina. |
| `BatteryChargingFull` | Bateria carregada/estado do dispositivo. | Explicação de recursos e permissões (`PermissionsScreen`). |
| `Bloodtype` | Sangue e tipo sanguíneo. | Resumo do Dashboard e tela Sinais Vitais. |
| `Bolt` | Energia ou intensidade. | Opções de sintomas em Sintomas e Saúde feminina. |
| `Cake` | Data de nascimento/aniversário. | Dados do Perfil (`ProfileScreen`). |
| `CalendarMonth` | Calendário mensal. | Componente de calendário mensal (`MonthlyCalendar`). |
| `CameraAlt` | Câmera. | Explicação de permissão de câmera (`PermissionsScreen`). |
| `Check` | Concluído, selecionado ou confirmado. | Alarmes, Exames, Saúde feminina e etapas do Perfil inicial; também ações na tela de alarme. |
| `CheckCircle` | Confirmação ou estado concluído. | Dashboard, Gestação e Vacinas. |
| `ChevronLeft` | Navegar para o período anterior. | Calendário de Saúde feminina. |
| `ChevronRight` | Navegar para frente ou abrir detalhes. | Dashboard, calendário de Saúde feminina e Configurações. |
| `ChildCare` | Criança e cuidado infantil. | Saúde feminina/gestação e destino Minha Gestação na navegação. |
| `Circle` | Marcador circular/estado. | Indicadores ou seleção de sintomas (`SymptomScreen`). |
| `Close` | Fechar ou cancelar. | Dashboard, visualizador de imagem, Remédios, Perfil inicial, visualizador PDF, Receitas, Perfil e Sinais Vitais. |
| `CloudSync` | Sincronização com a nuvem. | Estado e ações da Conta Katu'I (`RemoteAccountScreen`). |
| `Compress` | Reduzir/compactar ou contrair. | Ações de visualização no Dashboard e em Sinais Vitais. |
| `Coronavirus` | Vírus ou agente infeccioso. | Categorização de exames (`ExamCategorizer`). |
| `Description` | Documento ou descrição. | Navegação para Exames e Importar Receita; cartões no Dashboard, central de saúde e Remédios. |
| `DeviceThermostat` | Temperatura corporal. | Dashboard e tela Sinais Vitais. |
| `DocumentScanner` | Digitalização de documento. | Importar ou capturar receita em Remédios (`MedicationScreen`). |
| `Delete` | Excluir ou remover. | Remoção de medicamentos, alarmes, consultas, exames, registros de saúde feminina, receitas, perfis, sintomas, vacinas e sinais vitais. |
| `Edit` | Editar registro. | Alarmes, Consultas, Saúde feminina, Remédios, Importar Receita, Receitas e Perfil. |
| `Event` | Evento ou compromisso agendado. | Atalhos do Dashboard, Consultas, seletor de data/hora e Gestação; navegação principal. |
| `EventAvailable` | Evento/data disponível. | Calendário e registros de Saúde feminina. |
| `ExpandLess` | Recolher conteúdo expandido. | Seções expansíveis em Vacinas (`VaccinationScreen`). |
| `ExpandMore` | Expandir conteúdo. | Seções expansíveis em Vacinas (`VaccinationScreen`). |
| `Face` | Pessoa ou expressão facial. | Registro de sintomas (`SymptomScreen`). |
| `Favorite` | Coração, cuidado ou favorito. | Dashboard, Saúde feminina, Sintomas e Sinais Vitais. |
| `Female` | Sexo feminino. | Perfil inicial, Perfil e dados de medicamentos (`OnboardingProfileScreen`, `ProfileScreen`, `MedicationScreen`). |
| `FileOpen` | Abrir arquivo. | Ações de arquivo em Configurações (`SettingsScreen`). |
| `Folder` | Pasta e documentos. | Explicação/permissões de arquivos (`PermissionsScreen`). |
| `Fullscreen` | Expandir para tela cheia. | Visualização de imagem ou resultado de exame (`ExameScreen`). |
| `Help` | Ajuda contextual. | Informações e orientações no Perfil (`ProfileScreen`). |
| `HelpOutline` | Ajuda ou explicação adicional. | Ajuda contextual em Exames, Saúde feminina, Configurações, Sintomas e Vacinas. |
| `HealthAndSafety` | Saúde e segurança. | Seção Saúde e Biometria do Perfil (`ProfileScreen`). |
| `Height` | Medida de altura. | Informação biométrica de altura no Perfil (`ProfileScreen`). |
| `History` | Histórico de registros. | Histórico de Alarmes e Saúde feminina. |
| `Home` | Página inicial. | Destino Home no menu principal (`MainActivity`). |
| `Image` | Imagem ou arquivo visual. | Categorização de exames e imagens de receitas (`ExamCategorizer`, `PrescriptionScreen`). |
| `Info` | Informações. | Destino Sobre o App (`MainActivity`). |
| `Inventory` | Inventário ou estoque. | Informações do aplicativo em Sobre (`AboutScreen`). |
| `KeyboardArrowDown` | Expandir ou mostrar itens abaixo. | Sobre o App e calendário mensal. |
| `KeyboardArrowLeft` | Ir para o mês anterior. | Navegação do calendário mensal. |
| `KeyboardArrowRight` | Ir para o próximo mês. | Navegação do calendário mensal. |
| `KeyboardArrowUp` | Voltar ao topo ou recolher. | Dashboard, Exames e calendário mensal. |
| `KeyboardDoubleArrowUp` | Adiar ou avançar repetidamente. | Ação de adiar na tela de alarme (`AlarmActivity`). |
| `Layers` | Camadas/organização de recursos. | Explicação de recursos na tela Permissões (`PermissionsScreen`). |
| `LocalFireDepartment` | Calor, ardor ou febre. | Categorias de sintomas em Sintomas e Saúde feminina. |
| `LocationOn` | Localização. | Endereço de consulta e localização do sintoma (`ConsultationScreen`, `SymptomScreen`). |
| `Male` | Sexo masculino. | Perfil inicial e Perfil. |
| `ManageSearch` | Busca detalhada. | Busca de sintomas (`SymptomScreen`). |
| `MedicalInformation` | Informações médicas. | Dados de saúde do Perfil (`ProfileScreen`). |
| `MedicalServices` | Atendimento ou serviço médico. | Identificação de Remédios e atalhos de saúde em Sobre, Alarmes, Consultas, Dashboard e Exames. |
| `Medication` | Medicamento ou dose. | Alertas, Dashboard, Saúde feminina, Remédios e Importar Receita. |
| `MonitorHeart` | Monitoramento cardíaco e sinais vitais. | Dashboard, central de saúde, Exames, tela Sinais Vitais e navegação. |
| `MonitorWeight` | Peso corporal. | Dashboard, Perfil e Sinais Vitais. |
| `MoodBad` | Humor ruim ou desconforto. | Sintomas de humor em Sintomas e Saúde feminina. |
| `MoreVert` | Mais opções. | Menu de ações de registros em Saúde feminina. |
| `MusicNote` | Som ou toque. | Configuração do som do alarme (`SettingsScreen`). |
| `Note` | Anotação ou observação. | Consultas, Importar Receita, Vacinas e Sinais Vitais. |
| `NotificationsActive` | Notificação ativa. | Explicação de permissões de notificação (`PermissionsScreen`). |
| `Notifications` | Notificações gerais. | Ícone de notificação associado a consultas na lista de Consultas (`ConsultationScreen`). |
| `Opacity` | Fluxo ou intensidade de líquido. | Categorização de exames, Saúde feminina e Sinais Vitais. |
| `OpenInNew` | Abrir fora da tela atual. | Abrir documentos PDF e links/recursos em Configurações. |
| `Person` | Pessoa ou profissional. | Profissional da consulta, Perfil e navegação/conta principal. |
| `PhoneAndroid` | Dispositivo móvel. | Informação sobre o dispositivo na tela Permissões. |
| `PhotoLibrary` | Galeria de fotos. | Seleção de imagem para importar receita (`PrescriptionImportScreen`). |
| `PictureAsPdf` | Documento PDF. | Exames, visualizador PDF e Receitas. |
| `Psychology` | Saúde mental e atividade psicológica. | Registros de humor em Sintomas e Saúde feminina. |
| `RadioButtonUnchecked` | Opção ainda não selecionada. | Estado de dose/registro de vacinação (`VaccinationScreen`). |
| `RemoveCircleOutline` | Remover item. | Remoção de itens ou resultados em Exames (`ExameScreen`). |
| `RocketLaunch` | Início ou ativação de recurso. | Apresentação de recursos na tela Permissões (`PermissionsScreen`). |
| `Save` | Salvar alterações. | Ações de salvamento em Configurações (`SettingsScreen`). |
| `Scale` | Peso ou medida corporal. | Dados de gestação e importação de receita. |
| `Schedule` | Horário ou agendamento. | Alarmes, consultas, Dashboard, seletor de data/hora, Saúde feminina e sintomas. |
| `Science` | Ciência e exames laboratoriais. | Categorização de exames (`ExamCategorizer`). |
| `Search` | Busca. | Busca de perfil e funções de procura no Perfil inicial e Perfil. |
| `Security` | Proteção ou segurança. | Orientações de segurança em Exames e Importar Receita. |
| `SentimentDissatisfied` | Humor insatisfeito. | Opção de humor em Sintomas e Saúde feminina. |
| `SentimentNeutral` | Humor neutro. | Opção de humor em Sintomas (`SymptomScreen`). |
| `SentimentSatisfied` | Humor satisfeito. | Opção de humor em Sintomas (`SymptomScreen`). |
| `SentimentVeryDissatisfied` | Humor muito negativo. | Opção de humor em Sintomas (`SymptomScreen`). |
| `Settings` | Configuração. | Destino Configurações e ações de Saúde feminina. |
| `Shield` | Proteção e segurança. | Informações de segurança em Sobre e Permissões. |
| `ShowChart` | Gráfico ou evolução. | Visualização de tendência de exame (`ExameScreen`). |
| `Snooze` | Adiar lembrete. | Ação de adiar o alarme (`AlarmActivity`). |
| `Straighten` | Altura ou comprimento. | Medição de altura em Sinais Vitais (`VitalsScreen`). |
| `SwitchAccount` | Trocar de pessoa ou perfil. | Alternância de perfil (`ProfileScreen`). |
| `Thermostat` | Temperatura. | Seleção/registro de sintoma e sensação térmica (`SymptomScreen`). |
| `Thunderstorm` | Tempestade ou estado intenso. | Clima/sensação em sintomas (`SymptomScreen`). |
| `Today` | Data atual. | Atalho para a data de hoje em Alarmes (`AlarmScreen`). |
| `Transgender` | Identidade/sexo informado. | Seleção de sexo no Perfil inicial e dados do Perfil. |
| `TrendingDown` | Tendência de queda. | Indicador de tendência de sintomas (`SymptomScreen`). |
| `TrendingUp` | Tendência de alta. | Indicador de evolução de exames (`ExameScreen`). |
| `Vaccines` | Vacinação. | Navegação e carteira de Vacinas (`MainActivity`, `HealthHubScreen`, `VaccinationScreen`). |
| `Visibility` | Mostrar conteúdo ou senha. | Alternância de visibilidade em Conta Katu'I e Configurações. |
| `VisibilityOff` | Ocultar conteúdo ou senha. | Alternância de visibilidade em Conta Katu'I (`RemoteAccountScreen`). |
| `Warning` | Aviso ou atenção necessária. | Alertas no Dashboard, Remédios, Gestação, Perfil e Vacinas. |
| `WarningAmber` | Aviso de atenção. | Avisos relacionados a sintomas (`SymptomScreen`). |
| `WaterDrop` | Água, fluxo ou glicose. | Indicadores do Dashboard e Saúde feminina. |
| `WbCloudy` | Nublado ou clima intermediário. | Seleção de humor/clima em Sintomas e Saúde feminina. |
| `WbSunny` | Sol e clima positivo. | Seleção de humor/clima em Sintomas e Saúde feminina. |
| `Woman` | Saúde feminina. | Destino Saúde Feminina e áreas de saúde feminina em Sobre, central de saúde e menu. |

## Android — imagens, ilustrações e ícones próprios

Os recursos abaixo são imagens ou drawables vetoriais do projeto. As variantes `mipmap-*-dpi` são cópias em resoluções diferentes do mesmo ícone, destinadas a tamanhos de tela distintos.

| Arquivo em `Icons/android/res/` | O que representa | Partes do app |
|---|---|---|
| `drawable/arms.png` | Ilustração dos braços. | Seleção/localização da região do corpo ao registrar sintomas (`SymptomScreen`). |
| `drawable/back.png` | Ilustração das costas. | Seleção/localização da região do corpo ao registrar sintomas (`SymptomScreen`). |
| `drawable/bandage.png` | Curativo. | Recurso gráfico de saúde disponível na pasta Android; não apareceu como referência direta nas telas examinadas. |
| `drawable/body-overview.png` | Silhueta/visão geral do corpo. | Opção geral para localizar sintomas (`SymptomScreen`). |
| `drawable/chest.png` | Ilustração do peito. | Seleção/localização da região do corpo ao registrar sintomas (`SymptomScreen`). |
| `drawable/legs.png` | Ilustração das pernas. | Seleção/localização da região do corpo ao registrar sintomas (`SymptomScreen`). |
| `drawable/stomach.png` | Ilustração do estômago. | Seleção/localização da região do corpo ao registrar sintomas (`SymptomScreen`). |
| `drawable/teeth.png` | Ilustração dos dentes. | Recurso gráfico de saúde disponível na pasta Android; não apareceu como referência direta nas telas examinadas. |
| `drawable/katui-app-mark.png` | Marca gráfica do Katu'I. | Botões de ação de notificações de medicamento, onboarding e marca do Dashboard (original `logo_katui_new.png`). |
| `drawable/katui-wordmark.png` | Logotipo horizontal com o nome Katu'I. | Recurso de marca; não apareceu como referência direta nas telas examinadas (original `logo_katui_png.png`). |
| `drawable/launcher-background.xml` | Fundo do ícone adaptativo do app. | Ícone do aplicativo Android (`mipmap-anydpi-v26/launcher-icon.xml` e `launcher-round-icon.xml`; original `ic_launcher_background.xml`). |
| `drawable/launcher-foreground.png` | Símbolo de saúde e pássaro da marca. | Primeiro plano do ícone do aplicativo Android, nas variantes adaptativas e de densidade (original `ic_launcher_foreground.png`). |
| `drawable/launcher-foreground-inset.xml` | Ajuste de margem do primeiro plano do launcher. | Composição do ícone adaptativo Android (original `ic_launcher_foreground_scaled.xml`). |
| `drawable/notification-status-icon.xml` | Símbolo branco monocromático para status de notificação. | Notificações de alarmes, consultas, sincronização e saúde feminina (original `ic_notification_custom.xml`). |
| `drawable/splash-screen-icon.png` | Símbolo da marca para inicialização. | Ícone da tela de abertura do Android (original `ic_splash_icon.png`). |
| `drawable/splash-screen-icon-padded.xml` | Ícone de abertura com margens seguras. | Splash screen do Android 12+ (original `ic_splash_icon_padded.xml`). |
| `drawable/splash-screen-brand.xml` | Composição de marca com margem para a tela de abertura. | Recurso alternativo de splash; não referenciado pelo tema ativo examinado (original `logo_splash.xml`). |
| `mipmap-*-dpi/launcher-icon.png` | Ícone do app em densidades diferentes. | Launcher Android; as variantes `mdpi`, `hdpi`, `xhdpi`, `xxhdpi` e `xxxhdpi` são tamanhos/resoluções do mesmo ícone (originais `ic_launcher.png`). |
| `mipmap-*-dpi/launcher-round-icon.png` | Variante redonda do ícone do app por densidade. | Launcher Android para dispositivos que usam ícone arredondado (originais `ic_launcher_round.png`). |
| `mipmap-*-dpi/launcher-foreground.png` | Primeiro plano do launcher por densidade. | Variantes de resolução do ícone do aplicativo (originais `ic_launcher_foreground.png`). |

## Ícones por menu

### Web — menu lateral principal

Os ícones acompanham a cor do texto do item: inativos `#6D827D`; em hover `#176B62` sobre `#F2F7F5`; ativos `#176B62` sobre `#E8F4F0`. Esses estados se ajustam com as regras do tema escuro.

| Grupo | Item | Ícone | Utilidade | Cor |
|---|---|---|---|---|
| Menu principal | Visão geral | `dashboard.svg` (`grid`) | Abre o painel com resumo, calendário e atalhos. | Estado do menu. |
| Menu principal | Medicamentos | `medication.svg` (`pill`) | Abre tratamentos e registros de medicamentos. | Estado do menu. |
| Menu principal | Alarmes | `notifications.svg` (`bell`) | Acessa lembretes. | Estado do menu. |
| Menu principal | Receitas | `receipt.svg` (`receipt`) | Acessa receitas cadastradas e leitura disponível. | Estado do menu. |
| Menu principal | Diário de sintomas | `health-journal.svg` (`journal`) | Acessa registros físicos e de humor. | Estado do menu. |
| Menu principal | Sinais vitais | `heart-rate.svg` (`pulse`) | Acessa medições de saúde. | Estado do menu. |
| Menu principal | Consultas | `calendar.svg` (`calendar`) | Acessa compromissos de saúde. | Estado do menu. |
| Menu principal | Exames | `document.svg` (`file`) | Acessa exames e resultados. | Estado do menu. |
| Sua saúde | Documentos | `folder.svg` (`folder`) | Abre os documentos de saúde. | Estado do menu. |
| Sua saúde | Saúde feminina | `calendar.svg` (`calendar`) | Abre ciclo e cuidados femininos; aparece para perfil habilitado. | Estado do menu. |
| Sua saúde | Carteira de vacinação | `vaccination.svg` (`vaccine`) | Acessa doses e histórico de vacinação. | Estado do menu. |
| Sua saúde | Perfis e dependentes | `user-profile.svg` (`profile`) | Gerencia pessoas sob cuidado; oculto no modo Guest. | Estado do menu. |
| Sua saúde | Backup pessoal | `document.svg` (`file`) | Acessa registros e ações de backup; oculto no modo Guest. | Estado do menu. |
| Rodapé | Ajuda | `help.svg` (`help`) | Abre a central de perguntas frequentes. | Estado do menu. |
| Rodapé | Configurações | `settings.svg` (`settings`) | Abre preferências, acessibilidade, privacidade e backup. | Estado do menu. |
| Rodapé | Sair da conta / do modo Guest | `arrow-right.svg` (`arrow`) | Encerra a sessão ou o modo Guest. | Estado do menu. |

### Web — menu de adição rápida

| Item | Ícone mostrado | Utilidade | Cor do símbolo / fundo |
|---|---|---|---|
| Sintoma | `✦`; `chevron-right.svg` (`chevron`) | Abre o formulário de sintoma. | Violeta `#8D80B4` / suave `#EFEBFA`. |
| Remédio | `＋`; `chevron-right.svg` (`chevron`) | Abre o formulário de medicamento. | Verde-azulado `#408D7D` / suave `#E2F2EE`. |
| Sinais vitais | `♥`; `chevron-right.svg` (`chevron`) | Abre opções para Glicose, Pressão arterial, Peso, Temperatura, Batimentos, Saturação e Altura. | Rosa `#BC6680` / suave `#FAE8EE`. |
| Consulta | `⌁`; `chevron-right.svg` (`chevron`) | Abre o agendamento de consulta. | Azul `#668EB6` / suave `#E8F0F8`. |
| Exame | `□`; `chevron-right.svg` (`chevron`) | Abre o cadastro de exame. | Rosa `#B55D87` / suave `#F9E7F1`. |
| Saúde feminina | `♡`; `chevron-right.svg` (`chevron`) | Abre Início de ciclo ou Sintomas e fluxo; aparece para perfil elegível. | Magenta-rosa `#B75D8C` / suave `#FAE5F1`. |
| Vacina | `✓`; `chevron-right.svg` (`chevron`) | Abre o cadastro de vacina. | Verde `#4C977F` / suave `#E4F3ED`. |
| Receita | `✦`; `chevron-right.svg` (`chevron`) | Abre cadastro manual no modo Guest ou leitura por IA nos demais modos. | Dourado `#AF8240` / suave `#F9F0DC`. |

O chevron indica que o item executa uma ação ou abre opções. Os itens de submenu não possuem ícones individuais. No tema escuro, o menu usa superfície `#21332F` e borda `#344B45`; os acentos de cada tipo continuam identificando a categoria.

### Web — menus de perfil e notificações

| Menu | Item/estado | Ícone | Utilidade | Cor |
|---|---|---|---|---|
| Seletor de perfil | Perfis disponíveis | Avatar com iniciais; sem arquivo de ícone. | Identifica e permite selecionar a pessoa ativa. | Cor do perfil selecionado; opções disponíveis: `#009C9C`, `#D89A27`, `#2F8FB0`, `#7054A5`, `#C66B88`, `#4A9A73`. |
| Seletor de perfil | Perfil selecionado | `checkmark.svg` (`check`) | Marca o perfil ativo. | Cor de texto/realce definida pelo tema do menu. |
| Seletor de perfil | Adicionar dependente | `add.svg` (`plus`) | Inicia o cadastro de dependente. | Cor de ação do tema. |
| Seletor de perfil | Sair da conta | `arrow-right.svg` (`arrow`) | Encerra a sessão. | Cor de ação do tema. |
| Próximos alarmes | Abrir painel | `notifications.svg` (`bell`) | Abre ou fecha o painel de notificações/alarmes. | Cor de ação da barra superior. |
| Próximos alarmes | Lembrete de medicamento | `medication.svg` (`pill`) | Distingue lembrete relacionado a medicamento. | Ícone do tipo de lembrete; sem valor hexadecimal específico identificado. |
| Próximos alarmes | Outro tipo de alarme | `notifications.svg` (`bell`) | Representa lembretes não associados a medicamento. | Ícone do tipo de lembrete; sem valor hexadecimal específico identificado. |
| Próximos alarmes | Ver todos os alarmes | `arrow-right.svg` (`arrow`) | Navega para a tela de alarmes. | Cor de ação do tema. |

### Android — menu inferior expansível

O menu Android apresenta três atalhos circulares no topo, cartões na seção “Área de Saúde” e atalhos de sistema. Os ícones são Material Icons; os nomes entre parênteses correspondem aos símbolos usados no código. Os cartões de saúde usam o acento da categoria no ícone, no fundo e na borda; a intensidade/transparência muda entre tema claro e escuro para preservar contraste.

| Grupo | Item | Ícone Android | Utilidade | Cor |
|---|---|---|---|---|
| Atalhos principais | Home | `Home` | Abre o dashboard. | Primária do tema (padrão `#009688`). |
| Atalhos principais | Perfil | `Person` | Abre os dados do perfil. | Secundária do tema (padrão `#00B0FF`). |
| Atalhos principais | Alarmes | `Alarm` | Abre a gestão de lembretes. | Terciária do tema ativo. |
| Área de Saúde | Remédios | `MedicalServices` | Abre o controle de medicamentos. | Teal `#009688`. |
| Área de Saúde | Sintomas | `Assignment` (variante espelhada `AutoMirrored`) | Abre o registro e histórico de sintomas. | Roxo `#673AB7`. |
| Área de Saúde | Sinais Vitais | `MonitorHeart` | Abre as medições de saúde. | Vermelho `#F44336`. |
| Área de Saúde | Consultas | `Event` | Abre agendamentos médicos. | Azul-ciano `#00B0FF`. |
| Área de Saúde | Exames | `Description` | Abre o histórico de exames. | Rosa `#E91E63`. |
| Área de Saúde | Saúde Feminina | `Woman` | Abre ciclo e saúde íntima; incluído apenas para perfil feminino ou intersexo. | Magenta `#FF4081`. |
| Área de Saúde | Receitas | `Article` | Abre receitas e prescrições. | Laranja `#FF9800`. |
| Área de Saúde | Vacinas | `Vaccines` | Abre a carteira de vacinação. | Verde `#4CAF50`. |
| Configurações | Configurações | `Settings` | Abre preferências e controles do app. | Secundária do tema (padrão `#00B0FF`). |
| Configurações | Sobre o App | `Info` | Exibe informações sobre o aplicativo. | Secundária do tema (padrão `#00B0FF`). |

O cartão também apresenta subtítulo com a finalidade do módulo. A seção “Trocar perfil ativo” mostra iniciais e nome, sem ícone próprio; o perfil ativo usa a cor do usuário, e os inativos usam cor neutra do tema. Os fundos e bordas são translúcidos: no tema claro, alfa `0.08`/`0.2`; no escuro, alfa `0.28`/`0.55`, respectivamente.

## Observações

- Os nomes descritivos em `Icons/` são cópias para consulta; renomear esses arquivos não altera os nomes nem as referências dos recursos originais usados pelo aplicativo.
- Os ícones Android listados são símbolos da biblioteca Material Compose, portanto não possuem um arquivo de imagem próprio copiado para `Icons/`.
- A aplicação web não apresenta referências a ícones remotos por URL no código examinado; usa os SVGs inline e os arquivos locais descritos acima.
- Imagens de exames, receitas, PDFs ou documentos escolhidos pelo usuário são conteúdo variável e não fazem parte deste inventário de ícones da interface.
