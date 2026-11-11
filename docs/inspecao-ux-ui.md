# Inspeção de UX/UI — ReadRace

Data: 11/11/2026. Inspeção visual e de código, sem mudanças na interface.

A biblioteca tem conteúdo, mas oferece pouca informação útil por livro. A sensação de vazio vem principalmente do cabeçalho alto, das capas sem identificação e da ausência de uma ação clara de continuar a leitura. A recomendação é reorganizar a hierarquia antes de acrescentar decoração ou novas seções.

## Método e limites

Aplicação Expo Web executada localmente com API real e banco descartável contendo o seed do projeto. Navegação com Chromium/Playwright em 390 × 844; biblioteca também em 320 × 740. Inspecionados Feed/Comunidades, biblioteca, busca inicial, desafios, perfil, adicionar livro, página de clube e detalhe de livro. Estado de biblioteca vazia simulado interceptando somente a resposta da biblioteca.

As capturas são evidências do ambiente local, não de produção. Capas externas demoraram a aparecer na primeira navegação e carregaram posteriormente; isso não comprova indisponibilidade permanente. Falta de fallback foi confirmada no código. Não foram auditados todos os modais, estados de erro, teclado virtual, leitor de tela, desktop amplo ou Android/iOS nativos. As recomendações são avaliação heurística, ainda sem teste de usabilidade com leitores.

## Achados priorizados

Prioridade alta significa impacto direto na identificação de conteúdo ou na tarefa principal; média indica clareza e hierarquia; baixa indica refinamento visual. Não são classificações de bugs bloqueantes.

| Prioridade | Tela e evidência | Problema e consequência | Recomendação |
|---|---|---|---|
| Alta | [Biblioteca carregada](ux-ui/11-biblioteca-carregada.png) | Capas de 80 × 100 px sem título, autor ou estado visível. É preciso reconhecer a edição ou abrir o livro para identificá-lo. | Mostrar título e autor; dar mais espaço aos livros em leitura. Preservar identificação mesmo sem imagem. |
| Alta | Biblioteca e `BookCover.tsx` | A imagem ausente ou com erro deixa apenas o fundo cinza. Durante o carregamento observado, a biblioteca parecia composta por blocos vazios. | Usar fallback com título e ícone; distinguir carregamento de falha. Não depender da capa para comunicar qual é o livro. |
| Alta | Biblioteca | Não há progresso nem atalho explícito para registrar leitura na listagem. A tarefa central fica escondida no detalhe. | Colocar “Continuar lendo” primeiro, com páginas/progresso e “Registrar leitura”. Exige ampliar o contrato da API para evitar uma requisição por livro. |
| Média | Biblioteca | Cabeçalho tem altura mínima de 192 px para duas linhas, ocupando cerca de 23% da viewport de 844 px. Favoritos vêm antes dos livros em leitura. | Usar cabeçalho compacto e priorizar leitura atual. Manter a identidade vinho e os cantos arredondados. |
| Média | Biblioteca | “Últimos livros” exibe apenas a lista `lendo`, não todos os livros recentes. | Renomear para “Lendo agora”, ou alterar os dados se a intenção for realmente mostrar atividade recente. |
| Média | Biblioteca | “Adicionar livro” está no fim do conteúdo rolável; não aparece na primeira viewport da biblioteca populada. | Colocar ação visível no topo ou em área fixa que não concorra com a navegação inferior. |
| Média | [Biblioteca vazia simulada](ux-ui/12-vazia-simulada.png) | Começa com “Livros favoritos” e um cartão “adicionar favorito”, seguido de outra chamada para adicionar livro. A prioridade inicial fica confusa. | Um único convite: “Sua próxima leitura começa aqui”, explicação curta e “Adicionar primeiro livro”. Mostrar favoritos depois de haver conteúdo. |
| Média | [Biblioteca em 320 px](ux-ui/06-biblioteca-320.png) | A grade passa de quatro para três colunas; o quarto livro ocupa outra linha. O cartão de adicionar favorito fica praticamente fora da área visível no carrossel. | Tratar três colunas como layout suportado; não colocar uma ação importante somente no fim do carrossel. |
| Média | [Adicionar livro](ux-ui/07-adicionar.png) | Estado inicial contém só campo de busca e um grande espaço vazio. Pode parecer uma tela inacabada. | Acrescentar instrução curta, exemplo de busca e explicação sobre escolher a lista depois. Sugestões/recentes somente se houver dados reais e intenção de produto. |
| Média | [Busca](ux-ui/03-busca.png) | Subtítulo menciona leitores e comunidades, mas também existe busca de livros. Na aba Usuários, placeholder fala de usuário ou comunidade apesar da seleção de uma categoria. | Ajustar subtítulo e placeholder à categoria selecionada. Manter o estado inicial com instrução, sem fabricar resultados. |
| Média | [Clube](ux-ui/08-clube.png) | Registrar leitura, quiz, fórum e chat têm o mesmo peso visual em quatro botões vinho consecutivos. | Manter registrar leitura como ação principal; apresentar quiz com seu estado e fórum/chat como ações secundárias. |
| Média | [Perfil](ux-ui/05-perfil.png) | Composição ilustrada ocupa aproximadamente 343 px antes dos dados. Avatar, estante, corpo e brasão competem visualmente; estatísticas ficam abaixo. | Reduzir altura da composição e tornar nome, nível e resumo de leitura acessíveis mais cedo. Preservar personalização. |
| Baixa | Navegação inferior nas capturas | Só ícones; a estrela não explicita “Desafios” para quem está começando. Os controles têm nomes acessíveis, mas não rótulos visuais. | Avaliar rótulos curtos sob os ícones e validar o espaço em 320 px. |
| Baixa | [Feed](ux-ui/01-feed.png) | “Meus Grupos” e “Criar Grupo” levam a funcionalidades chamadas de clube em outras telas. | Padronizar a terminologia entre feed, criação, notificações e detalhe. |

## Proposta de organização da biblioteca

Ordem recomendada, sem criar métricas ou recomendações artificiais:

1. Cabeçalho compacto “Meus livros”, com ação “Adicionar”.
2. “Lendo agora”: livro mais recente em destaque, título, autor, progresso e ação “Registrar leitura”. Outros livros em leitura continuam acessíveis na mesma seção.
3. Filtros por estado: “Lendo”, “Quero ler” e “Lidos”; favorito funciona como filtro adicional, pois o mesmo livro pode ser favorito e estar em qualquer estado.
4. Lista/grade da seleção, com título sempre visível. Favoritos podem ganhar uma fileira secundária se isso não duplicar excessivamente o conteúdo.
5. Estado vazio específico para o filtro, com ação contextual. Biblioteca totalmente vazia recebe um único convite de início.

O objetivo é permitir responder rapidamente: “O que estou lendo?”, “Onde parei?” e “Como registro a leitura?”. Não é necessário preencher toda a tela; espaço livre é adequado quando existe uma tarefa principal evidente.

## Implementação sugerida

**Primeira entrega — clareza com dados existentes:** títulos/autores visíveis, fallback de capa, nome “Lendo agora”, cabeçalho compacto, ação de adicionar no topo e estado vazio unificado. Revisar a finalidade do cartão “adicionar favorito”: hoje ele recebe o mesmo callback de adicionar livro.

**Segunda entrega — leitura como foco:** ampliar o retorno da biblioteca com progresso e informação suficiente para escolher a leitura recente. O contrato atual contém `livroId`, `titulo`, `autor`, `capaUrl` e `genero`; não contém páginas/progresso nem totais das listas. Não calcular contadores globais pelo tamanho da primeira página paginada. Reaproveitar o fluxo existente de registrar progresso, preservando regras de XP.

**Terceira entrega — consistência entre telas:** hierarquia de ações do clube, orientação da busca/adicionar livro, composição do perfil e terminologia. Desafios já tem uma estrutura relativamente clara de objetivo, progresso e recompensa; pode servir de referência de densidade informativa.

Arquivos principais: `mobile/src/features/biblioteca/MeusLivrosView.tsx`, `mobile/src/features/biblioteca/api.ts`, `mobile/src/components/BookCover.tsx`, `mobile/src/components/AppHeader.tsx`, `mobile/src/theme/tokens.js`, `mobile/src/features/busca/BuscaView.tsx`, `mobile/src/features/adicionarLivro/AdicionarLivroScreen.tsx`, `mobile/src/features/clube/PaginaClubeScreen.tsx` e `mobile/src/features/perfil/PerfilTopo.tsx`.

## Critérios de aceite para o redesenho

- Identificar cada livro pelo título mesmo quando a capa falhar.
- Ver a ação de adicionar na primeira viewport em 390 px e 320 px.
- Mostrar a leitura atual e sua ação principal antes de favoritos quando houver leitura em andamento.
- Diferenciar biblioteca totalmente vazia de uma lista/filtro sem itens.
- Manter acesso a todos os livros, paginação e favoritos após reorganizar a interface.
- Validar 0, 1, 4 e muitos livros, títulos longos, capa ausente, carregamento lento e falha de API.
- Validar conteúdo final da rolagem sem obstrução pela navegação inferior e acesso por teclado; conferir teclado virtual e fontes ampliadas em aparelho nativo.
- Ao adicionar registro direto, verificar persistência, regras de XP e atualização da biblioteca após retornar do detalhe.

Esta inspeção não altera as telas. As capturas e o relatório formam a base para uma implementação posterior e para regressão visual dos estados efetivamente aprovados.
