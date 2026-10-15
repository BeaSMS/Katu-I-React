# Arquitetura do Projeto

O Katu'I Offline segue os princípios de **Clean Architecture** adaptados para o ecossistema Android moderno, utilizando o padrão **MVVM (Model-View-ViewModel)**.

## Padrões de Design

### MVVM (Model-View-ViewModel)
- **View**: Implementada com Jetpack Compose. Observa estados expostos pelos ViewModels.
- **ViewModel**: Gerencia o estado da UI e interage com os Repositórios. Utiliza Hilt para injeção de dependências.
- **Model**: Composto pelas Entidades do Room e classes de domínio.

### Injeção de Dependência (Hilt)
O projeto utiliza Dagger Hilt para gerenciar a árvore de dependências.
- **[KatuiApplication.kt](../app/src/main/java/com/katui/offline/KatuiApplication.kt)**: Ponto de entrada anotado com `@HiltAndroidApp`.
- **Módulos de Injeção**:
    - `DatabaseModule`: Provedor do banco de dados Room e seus DAOs.
    - `DataStoreModule`: Provedor do DataStore para preferências do usuário.

## Fluxo de Dados
O fluxo de dados é preferencialmente unidirecional (UDF):
1. O usuário interage com a **UI**.
2. A UI chama métodos no **ViewModel**.
3. O ViewModel solicita dados ao **Repository**.
4. O Repository busca dados no **Room** ou **DataStore**.
5. O Room retorna `Flow` de dados, que o ViewModel converte em `State` para a UI.

## Navegação
A navegação é gerenciada pelo `NavHost` no [MainActivity.kt](../app/src/main/java/com/katui/offline/MainActivity.kt).
- Define rotas para todas as telas principais e secundárias.
- Utiliza uma `sealed class Screen` para centralizar a definição de rotas, títulos e ícones.

## Experiência de Inicialização e Splash Screen
O app utiliza a biblioteca `androidx.core:core-splashscreen` para garantir uma transição suave durante o carregamento inicial.
- O tema inicial é definido como `Theme.App.Starting` no manifesto.
- A inicialização é disparada no `onCreate` da `MainActivity` via `installSplashScreen()`.

## Multi-Processo e Confiabilidade
Para garantir que os alarmes funcionem de forma robusta e independente da interface principal:
- **Processo `:alarm`**: Os serviços e receptores de alarme rodam em um processo separado, minimizando o uso de memória e evitando que falhas na UI afetem as notificações críticas.
- **Processo `:sync`**: (Planejado/Em implementação) Para tarefas pesadas de sincronização de dados.

## Localização dos Arquivos
- **di/**: Módulos do Hilt.
- **ui/viewmodel/**: ViewModels da aplicação.
- **data/repository/**: Implementação dos repositórios que abstraem a fonte de dados.
