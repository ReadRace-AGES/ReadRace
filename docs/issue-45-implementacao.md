# Perfil público — #45

`GET /api/usuarios/{usuarioId}/perfil` consulta o usuário exibido, inclusive quando
ele é o usuário atual. Não recebe a identidade de quem está olhando. Usuários
inexistentes ou excluídos retornam 404 `USUARIO_NAO_ENCONTRADO`.

A API agrega seguidores, seguindo, livros lidos e páginas máximas; lista todas
as conquistas com seu estado e os favoritos com autores ordenados. Não altera
dados, não concede XP e não recalcula nível ou sequência. Não há migration.

## Ajustes autorizados ao escopo

- `xpAtual` contém `usuario.xp_total`. `xpDoNivel` foi omitido porque o schema não
  tem esse valor e não foi autorizada uma curva nova. A tela mostra o XP total,
  sem denominador nem fração de progresso; o anel identifica o nível persistido.
- A ilustração do topo não faz parte desta entrega. O cabeçalho usa o tema e o
  avatar existente.

## Mobile

A Busca já navega para `/usuario/{usuarioId}`. A nova rota está no stack da Busca,
preserva a navbar e retorna à busca. A aba do próprio perfil continua inalterada.
Seguidores, Seguindo, Ver mais e favoritos chamam o Toast existente. A tela
não oferece edição, adesão, Mascotes, Configurações ou botão de seguir.

Conquistas bloqueadas continuam na grade de duas colunas. Sem favoritos, a seção
é omitida. Erros exibem mensagem e tentativa novamente; o voltar permanece
disponível. Consultas antigas são canceladas e suas respostas são ignoradas.

## Como validar

1. Subir banco/API com o seed e abrir Busca → Usuários → resultado.
2. Consultar `/api/usuarios/00000000-0000-0000-0000-000000000001/perfil`:
   nível 5, XP 2450, 5 livros lidos, soma das páginas máximas, 5 conquistas
   desbloqueadas em 6 e 3 favoritos.
3. Abrir o usuário `00000000-0000-0000-0000-000000000002`: biblioteca vazia,
   conquistas bloqueadas e favoritos ocultos são esperados no seed.
4. Testar 404, falha de rede, nova tentativa, troca rápida de perfil e voltar.
5. Conferir os quatro pontos de Toast e a navegação com a navbar.

Automação: Maven `verify`, `npm test`, `npx tsc --noEmit`.
A conferência visual e de acessibilidade em dispositivo deve complementar os testes.

## Resultado da validação nesta entrega

- Maven `verify` com Java 21 e PostgreSQL via Testcontainers: aprovado, incluindo
  os 4 testes de integração de perfil e o limite de cobertura.
- Suítes mobile existentes e 4 novos testes de perfil: aprovados.
- TypeScript, Prettier dos arquivos mobile alterados e `git diff --check`: aprovados.
- Exportação web do Expo: aprovada, incluindo `/usuario/[usuarioId]`.
- Validação visual/interação em dispositivo: pendente.
