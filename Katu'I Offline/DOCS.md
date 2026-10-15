# Katu'I Offline - Documentação do Projeto

Este projeto é uma aplicação Android offline focada em saúde pessoal, permitindo que os usuários gerenciem seus medicamentos, alarmes, consultas, exames, vacinas e muito mais, com total privacidade.

## Visão Geral

Katu'I Offline foi desenvolvido utilizando as tecnologias mais modernas do ecossistema Android, como Jetpack Compose para a UI, Room para persistência local de dados e Hilt para injeção de dependências.

## Estrutura da Documentação

A documentação está dividida em seções para facilitar a navegação:

1.  **[Arquitetura](docs/architecture.md)**: Detalha a estrutura do projeto, MVVM, Injeção de Dependências e Navegação.
2.  **[Camada de Dados](docs/data_layer.md)**: Explica o Banco de Dados Room, Entidades, DAOs e Repositórios.
3.  **[Camada de UI](docs/ui_layer.md)**: Descreve as telas (Screens), ViewModels e Componentes reutilizáveis.
4.  **[Utilitários e Infraestrutura](docs/util_and_infra.md)**: Cobre funcionalidades como OCR, PDF, Backup no Google Drive e Gerenciamento de Alarmes.

## Principais Funcionalidades

- **Dashboard Inteligente**: Resumo das próximas doses, consultas e insights baseados em diretrizes de saúde.
- **Medicamentos e Estoque**: Gerenciamento completo com catálogo de +300 itens, controle de estoque automático e horários.
- **Alarmes**: Notificações persistentes e tela de alerta dedicada para medicamentos.
- **Saúde Feminina**: Controle integrado de ciclo menstrual, previsões de período fértil e lembretes de anticoncepcionais.
- **Exames e Receitas**: Importação via PDF ou imagem com suporte a OCR para preenchimento automático.
- **Sinais Vitais**: Registro granular e interpretação de pressão, glicose, peso, oximetria e temperatura com gráficos históricos.
- **Inteligência Offline**: Motor de correlação entre sintomas e medicamentos (efeitos colaterais) e dicas de saúde do Ministério da Saúde.
- **Backup e Privacidade**: Sincronização manual com Google Drive e total controle dos dados localmente.
- **Experiência Fluida**: Splash screen moderna e interface otimizada para acessibilidade (fontes para dislexia e tamanhos variáveis).

---
Desenvolvido com foco em acessibilidade e privacidade.
