# Design de Estoque e Insights Inteligentes (MS)

Este documento detalha a implementação técnica para o controle de estoque de medicamentos e o motor de mensagens baseado nas diretrizes do Ministério da Saúde.

## 1. Controle de Estoque e Catálogo (3NF)

Para tornar o sistema preciso e facilitar o preenchimento, utilizaremos uma estrutura normalizada.

### Mudanças no Banco de Dados (3NF):
#### [NEW] `MedicamentoCatalogo`
Tabela de referência para autocomplete (dados da ANVISA simplificados).
- `id`: Long
- `nome`: String
- `principioAtivo`: String
- `registroMS`: String

#### `Medicamento`
- `catalogoId`: Long? (Link para a tabela acima)
- `estoqueAtual`: Double
- `estoqueMinimo`: Double
- `dosePadrao`: Double (Quantidade subtraída a cada tomada)

### Fluxo de Autocomplete:
- Ao digitar na tela de Medicamentos, o app busca em `MedicamentoCatalogo` (offline).
- Inicialmente, incluiremos uma lista com os **~50 medicamentos mais comuns** no Brasil (Dipirona, Paracetamol, Losartana, etc.) como um "starter kit".

---

## 2. Mensagens "Smart" (Ministério da Saúde)

O sistema de mensagens será dividido em categorias, selecionando frases aleatórias ou baseadas em gatilhos específicos.

### Categorias e Fontes:
| Categoria | Gatilho | Exemplo de Mensagem (MS) |
| :--- | :--- | :--- |
| **Adesão** | Tomou todos os remédios do dia | "Você está cuidando bem de você e de quem você ama! Continue assim." |
| **Alimentação** | Manhã/Início do dia | "Prefira sempre alimentos *in natura*. Descasque mais e desembale menos." |
| **Atividade Física** | Baixo registro de vitais | "Todo movimento conta. Que tal subir as escadas hoje?" |
| **Saúde Mental** | Final do dia | "A vida pede pausa. A saúde agradece. Tenha um bom descanso." |
| **Prevenção** | Idade/Sexo (Checkup) | "Mantenha sua vacinação em dia. Vacina é vida, vacina é para todos." |

### Arquitetura do Motor de Insights:
- **InsightProvider**: Uma classe utilitária que recebe o perfil do usuário e o histórico recente para decidir qual mensagem exibir no Dashboard.

---

## 3. Plano de Ação

### Fase 1: Database (Estoque)
1.  Atualizar a entidade `Medicamento`.
2.  Criar migração para o Room.
3.  Atualizar o `AlarmeDao` para que ao marcar como `tomado`, dispare um `update` na quantidade do `Medicamento` relacionado.

### Fase 2: UI (Dashboard Smart)
1.  Criar o componente `SmartInsightCard` no Dashboard.
2.  Implementar a lógica de seleção de frases do Ministério da Saúde.
3.  Adicionar o progresso do estoque na tela de medicamentos.

### Fase 3: Validação
- Registrar um medicamento com 10 doses.
- Marcar 1 dose como tomada.
- Verificar se o estoque caiu para 9.
- Verificar se o alerta de "Reposição Necessária" aparece quando chegar no limite.
