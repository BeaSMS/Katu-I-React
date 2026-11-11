# Regras de arquitetura do frontend

## Organização

- Organize o código por responsabilidade e domínio: componentes de interface, hooks de estado e ciclo de vida, serviços de aplicação e integrações externas ficam em módulos separados.
- Mantenha `src/App.jsx` como ponto de composição da aplicação. Regras de negócio, estado complexo, chamadas de API e componentes de tela pertencem a módulos próprios.
- Prefira componentes pequenos e hooks focados. Extraia lógica compartilhada para um módulo único em vez de duplicá-la.
- Mantenha integrações HTTP e persistência fora dos componentes visuais. A interface deve chamar serviços ou ações com contratos explícitos.
- Use nomes e pastas que indiquem o propósito do módulo; evite arquivos genéricos que misturem domínios independentes.
- Escreva em português os nomes de variáveis, funções, métodos e classes definidos pelo projeto, inclusive nomes de classes CSS. Ao renomear classes CSS, atualize todos os usos no JSX/HTML e nos estilos.
- Dê nomes em português aos arquivos e pastas mantidos pelo projeto. Preserve apenas nomes exigidos por ferramentas, plataformas ou contratos externos; ao renomear um arquivo, atualize imports, scripts, registros de recursos e links da documentação.
- Preserve nomes exigidos por contratos externos, como campos e rotas da API, formatos persistidos, APIs do navegador e identificadores exportados por bibliotecas. Quando necessário, associe-os a identificadores locais em português sem alterar o contrato.
- Atualize os testes e a documentação do domínio quando uma mudança alterar comportamento ou contratos.

## Limite de tamanho

- Cada arquivo de código mantido no projeto deve ter no máximo **200 linhas**. Isso inclui JavaScript, JSX, TypeScript, CSS, código do servidor, testes e scripts.
- Em arquivos JSX, blocos JSX completos — incluindo atributos, expressões de renderização e texto HTML — não contam para o limite de linhas. O JavaScript fora desses blocos continua contando. Mantenha a marcação legível, com elementos aninhados e irmãos em linhas separadas e indentação consistente; use `npm run format:jsx`.
- Ao atingir 160 linhas, divida o arquivo antes de acrescentar outra responsabilidade.
- O limite é verificado por `npm run check:architecture`; essa verificação deve passar antes de concluir uma alteração.
- Arquivos gerados, dependências instaladas e arquivos de dados/lock não são módulos de código mantidos e ficam fora da verificação.

## Mudanças seguras

- Preserve o comportamento existente ao extrair módulos e valide com build, testes e verificação arquitetural.
- Evite extrações mecânicas que apenas renomeiem ou recortem arquivos sem criar limites claros entre responsabilidades.
- Não esconda erros de API, persistência ou inicialização com valores padrão silenciosos; apresente erros pelo fluxo já usado pelo app.
