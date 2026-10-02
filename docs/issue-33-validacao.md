# Validação da issue #33

Data: 13/09/2026. Referência: https://github.com/ReadRace-AGES/ReadRace/issues/33

**Resultado atualizado: defeitos do backend e do modal corrigidos. Build aprovado com 69 testes unitários e 118 de integração, incluindo regressões de bônus e concorrência. Integração com Detalhe (#32) e Clube (#35) registrada como dependência, conforme escopo confirmado pelo usuário.**

## Correções e validação final

- Bônus calculado pela página máxima histórica anterior: retroceder e concluir novamente não concede outros 150 XP. Novo teste de integração força `flush` e limpa o contexto de persistência entre as chamadas para conferir o estado gravado.
- Criação de item usa `INSERT ... ON CONFLICT (usuario_id, livro_id) DO NOTHING`, seguida da leitura com lock pessimista. Novo teste executa 12 chamadas simultâneas para criar o item e mais 12 para avançar: todas concluem, apenas um item é criado, 24 registros são gravados e cada grupo concede somente 10 XP no total.
- Modal abre vazio e preserva o texto digitado. Valores negativos, decimais, mistos, vazios ou fora do intervalo não habilitam o envio. A barra fica em zero para entradas inválidas.
- Trava síncrona impede duas submissões antes de o React atualizar o estado de carregamento. Campo, botões de fechar e gestos/overlay do BottomSheet ficam bloqueados durante a chamada; erros liberam nova tentativa e preservam o texto.
- Comandos aprovados: `mvnw.cmd --batch-mode spotless:apply spotless:check verify`, `tsc --noEmit`, formatação dos componentes alterados e `git diff --check`.
- Log final: `backend/target/ci-correcao.log`. Cobertura mínima atendida. A validação visual não pôde ser executada: o navegador da sessão não disponibilizou nenhuma instância conectada.
- A rota `/teste` permanece disponível para validação manual. Conectar o modal às telas #32/#35 e executar os respectivos fluxos continua sendo dependência; essas telas não existem neste checkout e sua implementação não faz parte desta correção.

As seções abaixo preservam o diagnóstico e as evidências **anteriores às correções**. Os resultados negativos do arquivo `issue33-validacao.json` são históricos, não o resultado da suíte final.

## Método e limites

- Build com Java 21: `mvnw.cmd --batch-mode spotless:apply spotless:check verify` — 69 testes unitários, 116 de integração, sem falhas; cobertura mínima atendida.
- Mobile: `tsc --noEmit` — aprovado.
- API executada a partir do JAR gerado, na porta 18081, com PostgreSQL 16 temporário e independente do banco de desenvolvimento.
- Fixture exclusiva nesse banco: livro de 223 páginas, página atual e máxima iniciais 100. Foram realizadas requisições HTTP reais, com commit, e comparação dos dados antes/depois.
- Mobile revisado pelo código. Não houve execução em dispositivo/emulador nem comparação visual com Figma. Os fluxos de Detalhe e Clube não estão conectados ao componente neste checkout.
- Evidências locais geradas: `backend/target/issue33-validacao.json`, `backend/target/issue33-api.log`, `backend/target/ci-verificacao.log`; o roteiro executável está em `backend/target/validar-issue33.cjs`. Arquivos em `target` são temporários e podem ser removidos por `mvn clean`.

## Problemas encontrados

### Alta: bônus de conclusão pode ser concedido novamente

Reprodução: alcançar 223 → informar 100 → informar 223 novamente.

Esperado na última chamada: `xpPaginas=0`, `xpConclusao=0`, `xpTotal=0`.

Obtido por HTTP: `200`, `xpPaginas=0`, **`xpConclusao=150`, `xpTotal=150`**.

Causa: `RegistrarProgressoService.java:60` usa `estaConcluido()` como memória de conclusão. Essa função depende do status e da página atual; `ItemBiblioteca.registrarProgresso()` muda o status para `lendo` ao retroceder. A página máxima conserva a informação histórica, mas não é usada para decidir se o bônus já foi concedido.

Correção indicada: decidir o primeiro bônus pela máxima anterior em relação ao total; incluir regressão para concluir → retroceder → concluir, além da repetição imediata já testada. A política de status ao retroceder deve ser distinguida da memória necessária para não repetir o bônus.

### Alta: primeiro registro concorrente retorna erro interno

Reprodução: 12 chamadas simultâneas com `pagina=10` para o mesmo livro ainda fora da biblioteca.

Obtido: **3 respostas 200 e 9 respostas 500**, com envelope `INTERNAL_ERROR`. O log confirma violação de `uq_item_biblioteca_usuario_livro`. A soma de XP das respostas bem-sucedidas foi 10, sem duplicação de XP nesse ensaio.

Causa: o lock pessimista em `ItemBibliotecaRepository.java:83` protege itens existentes. Quando o item não existe, chamadas concorrentes podem executar o `save` do `orElseGet` em `RegistrarProgressoService.java:54`.

Correção indicada: tornar a criação concorrente segura (por exemplo, inserção com tratamento de conflito seguida de leitura bloqueada) e testar requisições em transações independentes. Este é um caso adicional de robustez; a issue não apresenta uma tabela específica de concorrência, mas exige prevenção de duplicidade e comportamento consistente.

### Alta: entrada inválida vira outra página válida no modal

Em `mobile/src/features/progresso/RegistrarProgressoSheet.tsx:134`, `value.replace(/\D/g, "")` transforma `-1` em `1`, `1.5`/`1,5` em `15` e `abc12` em `12`.

Consequência: ao digitar ou colar uma entrada inválida, o botão pode habilitar e enviar uma página diferente. A issue exige bloqueio de entradas negativas e não inteiras. O backend recebe o valor já alterado, portanto não consegue rejeitar o valor original.

Correção indicada: preservar o texto e validar seu formato e intervalo antes de habilitar o envio. Comprovação por inspeção do manipulador; interação no dispositivo ainda pendente.

### Alta: origens reais ainda não integradas

A única utilização de `RegistrarProgressoSheet` está em `mobile/src/app/teste.tsx:244`. Não há ligação ao bloco “Seu progresso” do Detalhe do livro nem ao botão “Registrar leitura” do Clube neste checkout.

O callback de sucesso da rota de teste atualiza página atual, máxima e percentual. Isso não demonstra o aceite dos dois fluxos reais. As telas #32 e #35 são dependências externas ao escopo de construção da #33, mas sua ausência impede validar esses critérios de ponta a ponta e considerar a entrega completa.

### Média: modal abre preenchido em vez do estado vazio previsto

Em `RegistrarProgressoSheet.tsx:60`, a abertura preenche o campo com `paginaAtual` quando ela é positiva. Para um livro em leitura, o placeholder não aparece, a barra não começa em 0% e o botão pode abrir habilitado. O roteiro “Como validar”, passos 1 e 2, pede placeholder/campo em branco e botão desabilitado nesse fluxo.

Correção indicada: iniciar a entrada vazia ao abrir, conforme o estado documentado, ou esclarecer formalmente a divergência na especificação.

## Casos aprovados pela API real

| Sequência (livro de 223 páginas) | XP páginas | XP conclusão | Máxima | Resultado |
|---|---:|---:|---:|---|
| 100 → 145 | 45 | 0 | 145 | Aprovado |
| 145 → 100 | 0 | 0 | 145 | Aprovado |
| 100 → 145 | 0 | 0 | 145 | Aprovado |
| 145 → 200 | 55 | 0 | 200 | Aprovado |
| 200 → 223 | 23 | 150 | 223 | Aprovado; total 173, concluído |
| 223 → 223 | 0 | 0 | 223 | Aprovado |

Também conferidos: página atual, total, percentual arredondado, XP total e indicador de conclusão em todas essas respostas.

- `300`, `0`, `-1`, `1.5`, `"abc"`, `null`, booleano, objeto, array, inteiro fora do intervalo e campo ausente: **422 / PAGINA_INVALIDA**, com `message` textual.
- Livro inexistente: **404 / LIVRO_NAO_ENCONTRADO**.
- As chamadas inválidas e o 404 não alteraram `item_biblioteca` nem `registro_leitura`, conforme comparação integral dessas tabelas.
- Uma tentativa SQL direta de reduzir a máxima foi recusada pela trigger `validar_pagina_maxima`.
- Comparação de todas as outras tabelas antes/depois: nenhuma mudança, incluindo usuário, sequência/nível, posts, desafios, conquistas e membros/pontos de clube. XP permanece calculado e devolvido; não foi somado ao usuário.

## Mobile: comportamentos presentes e validação pendente

Por inspeção, o componente apresenta título/pergunta/placeholder, total do livro, barra/percentual, indicador de carregamento e campo não editável durante envio. Envia apenas a página ao endpoint, sem cálculo/exibição de XP. O `catch` mantém o texto digitado e exibe `ApiError.message`; o callback de sucesso só é chamado depois da resposta bem-sucedida.

Ainda é necessário executar no dispositivo: falha de rede e tentativa novamente, toque duplo, fechamento/reabertura durante chamada, atualização das telas reais e fidelidade visual. A proteção contra toque duplo usa apenas estado React, sem trava síncrona; o `BottomSheet` também recebe `onClose` livre mesmo durante carregamento. Estes pontos requerem teste de interação antes de afirmar que os estados de carregamento/erro estão integralmente validados.

## Correções de CI realizadas nesta sessão

- Removido o reset SQL que tentava diminuir a máxima do seed de 145 para 100 e falhava na trigger antes dos oito testes de progresso.
- Testes passaram a partir do seed real, com rollback por teste e `flush()` antes do rollback para executar as escritas e restrições no banco.
- Spotless configurado com finais de linha UNIX para consistência entre Windows e Linux.
- A alteração de publicação de `failsafe-reports` foi revertida a pedido do usuário; o workflow permanece na versão original.

Os defeitos reproduzidos neste diagnóstico foram corrigidos conforme a validação final no início do documento. A entrega completa da issue ainda depende das telas #32/#35 e da validação visual/manual; o build aprovado cobre agora também as regressões de bônus e concorrência.
