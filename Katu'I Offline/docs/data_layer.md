# Camada de Dados

A camada de dados é responsável pela persistência local, cache e gerenciamento de estados globais. O Katu'I Offline é um app *offline-first*, utilizando Room para dados estruturados e DataStore para preferências.

## Banco de Dados (Room)

O [KatuiDatabase.kt](../app/src/main/java/com/katui/offline/data/local/KatuiDatabase.kt) é a base central que define as entidades e versões do banco.

### Entidades principais (`data/local/entities/`)
- **Usuario**: Informações do perfil (nome, tipo sanguíneo, condições de saúde).
- **Medicamento**: Cadastro de remédios, dosagem e periodicidade. Suporta controle de estoque automático.
- **Alarme**: Instâncias de alarmes configurados para medicamentos.
- **Sintoma**: Registros diários de sintomas.
- **Consulta / Exame / Receita**: Gerenciamento de histórico médico e documentos.
- **Vacina**: Calendário vacinal e doses aplicadas.
- **Vitals**: Registros de sinais vitais (Pressão, Glicose, Peso, Oximetria, Temperatura, etc).
- **CicloMenstrual / Anticoncepcional**: Dados para o controle de saúde feminina.

### Catálogos e Inteligência (3NF)
O app utiliza uma estrutura normalizada para permitir buscas inteligentes e correlações:
- **MedicamentoCatalogo**: Starter kit com mais de 300 medicamentos comuns (ANVISA) para autocomplete.
- **SintomaCatalogo**: Lista mestre de sintomas para padronização de registros.
- **EfeitoColateralCatalogo**: Mapeamento entre medicamentos e possíveis efeitos colaterais.
- **SaudeItemCatalogo**: Catálogo de alergias e condições crônicas pré-populado.
- **CuidadorPacienteCrossRef**: Estrutura para suporte a múltiplos perfis (Dependente/Cuidador).

### DAOs (`data/local/dao/`)
Cada entidade possui um DAO correspondente que define as operações de CRUD. Quase todas as consultas retornam `Flow<T>` para permitir atualizações em tempo real na UI.

## Repositórios (`data/repository/`)

Os repositórios atuam como uma camada de abstração entre o Banco de Dados e os ViewModels.
- **MedicamentoRepository**: Lógica complexa para calcular próximas doses e gerenciar estoque.
- **FemaleHealthRepository**: Centraliza a lógica de Ciclo Menstrual e Anticoncepcionais.
- **SaudeCatalogoRepository**: Gerencia o catálogo global de condições de saúde.

## Preferências e Sessão (`data/`)

- **[SessionManager.kt](../app/src/main/java/com/katui/offline/data/SessionManager.kt)**: Utiliza `DataStore` para armazenar preferências do usuário como:
    - Estado do Onboarding.
    - Configurações de Acessibilidade (Tamanho da fonte, Fonte para dislexia).
    - Tema (Dark/Light).
- **[SecureStorage.kt](../app/src/main/java/com/katui/offline/data/SecureStorage.kt)**: (Se implementado) Para armazenamento de dados sensíveis criptografados.

## Conversores (`data/local/Converters.kt`)
Utilizados pelo Room para converter tipos complexos (como `LocalDate`, `LocalTime` e `Enums`) em tipos primitivos suportados pelo SQLite.
