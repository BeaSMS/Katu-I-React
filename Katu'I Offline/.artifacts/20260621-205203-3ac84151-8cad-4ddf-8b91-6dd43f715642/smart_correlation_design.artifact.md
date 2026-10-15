# Design de Correlação Inteligente (Smart Correlation)

Este documento descreve como o Katu'i Offline pode identificar relações entre sintomas, medicamentos e ciclos hormonais de forma automática e elegante.

## 1. Catálogos Padronizados (3NF)

Para que a correlação funcione, tanto medicamentos quanto sintomas precisam falar a "mesma língua".

### A. Catálogo de Sintomas (`SintomaCatalogo`)
Criaremos uma lista mestre de sintomas comuns (baseada no MedDRA simplificado e linguagem do Ministério da Saúde).
- `id`: Long
- `nome`: String (ex: "Cefaleia", "Náusea", "Tosse Seca")
- `categoria`: String (ex: "Digestivo", "Neurológico", "Respiratório")
- `popular`: Boolean (para destacar os mais comuns no autocomplete)

### B. Mapeamento de Efeitos Colaterais (`EfeitoColateralCatalogo`)
Tabela de ligação que associa os 300 medicamentos aos sintomas do catálogo.
- `medicamentoId`: FK para `MedicamentoCatalogo`
- `sintomaId`: FK para `SintomaCatalogo`
- `frequencia`: String (Comum, Muito Comum)

---

## 2. Motor de Correlação Temporal (Offline)
O app analisará o **tempo** entre os registros para encontrar padrões.

### Gatilho 1: Cadastro de Sintoma
Ao registrar um sintoma do catálogo:
1. O app verifica medicamentos tomados nas últimas **24 horas**.
2. Se houver correspondência no `EfeitoColateralCatalogo`, sinaliza a correlação.

### Gatilho 2: Análise de Ciclo
O app verifica se o sintoma (ex: "Cólica") ocorre repetidamente no mesmo intervalo de dias do ciclo menstrual (ex: dias 1 a 3).

---

## 3. Interface Smart (Autocomplete e Insights)

1.  **Autocomplete de Sintomas**: Na tela de registro, ao começar a digitar "Dor", o app sugere "Dor de Cabeça", "Dor Abdominal", etc.
2.  **Aviso de Padrão**: No histórico de sintomas, se uma correlação for encontrada, o item ganha um pequeno selo informativo.
3.  **Relatório para o Médico**: Gera um resumo: *"Nota: O sintoma 'Náusea' foi relatado 4 vezes em coincidência com o uso de 'Metformina'."*

---

## Próximos Passos
1.  **Criar Entidades**: `SintomaCatalogo` e `EfeitoColateralCatalogo`.
2.  **Popular Dados**: Inserir os ~50 sintomas mais comuns e mapear os efeitos dos 300 medicamentos (foco nos top 50 inicialmente).
3.  **Implementar Logica**: `CorrelationEngine` para detectar os cruzamentos de dados.
