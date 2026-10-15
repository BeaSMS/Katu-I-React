# Proposta de Funcionalidades "Smart" e Normalização de Banco de Dados

Esta proposta visa tornar o Katu'i Offline mais inteligente e sua base de dados mais robusta e eficiente (3NF).

## 1. Normalização do Banco de Dados (3NF)

Atualmente, algumas tabelas utilizam campos de texto livre para dados que se repetem ou listas separadas por vírgula (JSON implícito), o que dificulta filtros e análises.

### Mudanças Propostas:
- **Tabela `Especialidades`**: Separar as especialidades médicas de `Consulta` para uma tabela própria. Isso permite sugestões inteligentes e estatísticas por área.
- **Tabela `Locais`**: Separar locais de consulta para evitar repetição de strings e permitir busca por histórico de locais.
- **Normalização de Sintomas**:
    - Criar `SintomaCatalog` (lista mestre de sintomas comuns).
    - Criar `SintomaRegistro` que referencia o catálogo. Isso permite correlações automáticas entre sintomas e outros fatores (ex: "Você costuma sentir dor de cabeça 2 dias após esquecer o remédio X").
- **Medicamentos e Estoque**:
    - Separar a configuração do medicamento do registro de estoque.
    - Criar `MedicamentoEstoque` para rastrear quantidade restante.

---

## 2. Funcionalidades "Smart" (Inteligência Offline)

O objetivo é transformar dados passivos em insights ativos.

### A. Dashboard Preditivo e Dinâmico
- **Resumo Inteligente**: Em vez de apenas listar números, exibir frases como: "Você tomou todos os remédios nos últimos 3 dias! 🎉" ou "Sua pressão subiu um pouco hoje em relação à média da semana".
- **Alerta de Estoque**: Notificar quando um medicamento estiver chegando ao fim com base nos registros de `Alarme`.

### B. Correlações Automáticas
- **Sintoma vs. Medicamento**: Se o usuário registrar um sintoma "Náusea" logo após registrar que tomou um novo remédio, o app pode destacar essa possível relação.
- **Vitals vs. Atividade**: Correlacionar frequência cardíaca alta com sintomas de ansiedade registrados.

### C. Importação e Exportação Inteligente
- **Relatórios para Médicos**: Gerar um PDF resumido com gráficos de sinais vitais, frequência de sintomas e adesão aos medicamentos para levar na próxima consulta.
- **OCR para Receitas (Futuro)**: Já existe um `PrescriptionImportScreen`, podemos planejar a integração de reconhecimento de texto local para preencher os campos automaticamente.

### D. Saúde Feminina Integrada
- **Previsão de Sintomas**: Alertar a usuária sobre possíveis alterações de humor ou cólicas com base na fase do ciclo menstrual atual.

---

## Próximos Passos
1.  **Refatoração do Database**: Criar as novas entidades e migrações.
2.  **Lógica de Negócio**: Implementar os motores de correlação no ViewModel ou em UseCases dedicados.
3.  **UI Smart**: Atualizar o Dashboard para exibir esses novos insights.

> [!NOTE]
> Todas as funcionalidades serão mantidas **offline**, utilizando apenas lógica local e Room.
