# Camada de UI

A interface do usuário é construída inteiramente com **Jetpack Compose**, seguindo as diretrizes do Material Design 3.

## Estrutura da UI

A UI está organizada em:
- **Screens**: Telas completas que ocupam a maior parte da visualização.
- **ViewModels**: Detentores de estado para cada tela.
- **Components**: Widgets reutilizáveis.
- **Theme**: Definições de cores, tipografia e formas.

### Telas Principais (`ui/screens/`)

- **DashboardScreen**: Tela inicial com visão geral do dia, calendário interativo (com dados de saúde feminina), resumo de registros e motor de insights.
- **MedicationScreen**: Listagem, cadastro de medicamentos e gerenciamento de estoque.
- **PrescriptionImportScreen**: Fluxo de importação de receitas via câmera/galeria com processamento OCR.
- **VitalsScreen**: Registro granular de sinais vitais com seletor de período (Dia, Semana, Mês) e gráficos históricos.
- **FemaleHealthScreen**: Central de saúde da mulher com calendário menstrual, previsões e controle de pílula.
- **HealthHubScreen**: Hub central para acesso a sub-funcionalidades de saúde.
- **ProfileScreen**: Exibição do perfil do usuário e atalhos de configuração.
- **OnboardingProfileScreen / PermissionsScreen**: Fluxo inicial de configuração do app.

### ViewModels (`ui/viewmodel/`)

Cada tela complexa possui um ViewModel correspondente que gerencia seu `StateFlow`.
- **DashboardViewModel**: Consolida dados de várias fontes para a home.
- **MedicationViewModel**: Gerencia a lógica de adição/edição de remédios.
- **FemaleHealthViewModel**: Gerencia os estados complexos do ciclo menstrual.

### Componentes Reutilizáveis (`ui/components/`)

- **DateTimePicker**: Seletores customizados de data e hora.
- **MonthlyCalendar / WeeklyCalendar**: Visualizações de calendário para ciclo menstrual e agendamentos.
- **PdfViewerDialog / FullScreenImageDialog**: Visualizadores para documentos importados.

## Tema e Acessibilidade (`ui/theme/`)

O Katu'I Offline coloca grande ênfase em acessibilidade:
- **[Theme.kt](../app/src/main/java/com/katui/offline/ui/theme/Theme.kt)**: Suporta modo claro/escuro dinâmico.
- **Acessibilidade**:
    - Suporte a tamanhos de fonte variáveis configurados via `SessionManager`.
    - Opção de fonte específica para dislexia (**OpenDyslexic**).

## Navegação Global
A navegação é definida no `MainApp` dentro do [MainActivity.kt](../app/src/main/java/com/katui/offline/MainActivity.kt), utilizando o `NavHost` do Jetpack Navigation.
