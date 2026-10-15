# Utilitários e Infraestrutura

Esta seção cobre as funcionalidades de suporte, serviços de sistema e utilitários que viabilizam os recursos avançados do app.

## Gerenciamento de Alarmes

- **[AlarmScheduler.kt](../app/src/main/java/com/katui/offline/util/AlarmScheduler.kt)**: Utiliza o `AlarmManager` do Android para agendar notificações de medicamentos.
- **[AlarmReceiver.kt](../app/src/main/java/com/katui/offline/receiver/AlarmReceiver.kt)**: Captura os eventos do sistema e dispara a notificação ou a `AlarmActivity`.
- **[AlarmService.kt](../app/src/main/java/com/katui/offline/receiver/AlarmService.kt)**: Serviço para gerenciar o toque do alarme em primeiro plano.

## Processamento de Documentos

- **[OCRManager.kt](../app/src/main/java/com/katui/offline/util/OCRManager.kt)**: Utiliza ML Kit para extrair texto de imagens de receitas e exames.
- **[PdfManager.kt](../app/src/main/java/com/katui/offline/util/PdfManager.kt)**: Lida com a abertura e renderização de arquivos PDF.
- **[ExamCategorizer.kt](../app/src/main/java/com/katui/offline/util/ExamCategorizer.kt)**: Lógica para tentar categorizar exames com base no texto extraído.

## Backup e Nuvem

- **[GoogleDriveManager.kt](../app/src/main/java/com/katui/offline/util/GoogleDriveManager.kt)**: Interface com a API do Google Drive para upload/download do arquivo de banco de dados.
- **[BackupManager.kt](../app/src/main/java/com/katui/offline/util/BackupManager.kt)**: Orquestra a exportação do banco de dados SQLite para um arquivo físico e vice-versa.

## Inteligência e Lógica de Negócio

- **[VitalsInterpreter.kt](../app/src/main/java/com/katui/offline/util/VitalsInterpreter.kt)**: Fornece interpretações clínicas básicas (ex: "Normal", "Elevada") com base nos valores de sinais vitais inseridos.
- **[VacinaScheduleHelper.kt](../app/src/main/java/com/katui/offline/util/VacinaScheduleHelper.kt)**: Contém as regras do calendário vacinal do SUS para sugerir próximas doses.
- **Insight Engine**: Lógica integrada nos ViewModels para selecionar mensagens de saúde (MS) e detectar correlações entre sintomas e uso de medicamentos.

## Configuração do Projeto
- **Jetpack Compose 1.7+**: UI reativa e moderna.
- **Kotlin Serialization**: Para persistência e transferência de dados estruturados.
- **Android SDK 36 (VanillaIceCream)**: Alinhado com as APIs mais recentes.

- **[build.gradle.kts (Project)](../build.gradle.kts)**: Configurações de nível de projeto e plugins (Hilt, Kotlin, KSP).
- **[build.gradle.kts (App)](../app/build.gradle.kts)**: Dependências detalhadas, versões do SDK e configurações de build.
- **[AndroidManifest.xml](../app/src/main/AndroidManifest.xml)**: Declaração de atividades, serviços, permissões e receptores.
