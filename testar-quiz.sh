#!/usr/bin/env bash
# Teste manual da #161 (publicar quiz do clube) contra a API rodando de verdade.
#
#   ./testar-quiz.sh           roda a bateria inteira (API + checagens do app da revisao da PR 190)
#   ./testar-quiz.sh limpar    apaga so o que este script criou (titulos "TESTE-161...")
#
# Pre-requisitos: `docker compose up --build -d` na raiz do repositorio, com AUTH_MODO=seed
# (o padrao), que faz o usuario atual ser o Daniel, lider dos clubes 1 e 2.
# Variaveis opcionais: API, DB_CONTAINER, DB_USER, DB_NAME.

API="${API:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-readrace-db}"
DB_USER="${DB_USER:-readrace}"
DB_NAME="${DB_NAME:-readrace}"

CLUBE_LIDER="50000000-0000-0000-0000-000000000001"   # Daniel e administrador
CLUBE_MEMBRO="50000000-0000-0000-0000-000000000003"  # Daniel e membro comum
CLUBE_FORA="50000000-0000-0000-0000-000000000004"    # Daniel nao participa
CLUBE_INEXISTENTE="50000000-0000-0000-0000-0000000000ff"

PERGUNTA='{"enunciado":"Quem escreveu?","alternativas":["A","B","C","D"],"correta":2}'
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

OK=0
FALHAS=0

sql() {
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$1" 2>/dev/null | tr -d '[:space:]'
}

# post <clubeId> <json>  ->  preenche CODIGO e CORPO
post() {
  CODIGO=$(curl -s -o "$TMP" -w '%{http_code}' -X POST "$API/api/clubes/$1/quiz" \
    -H 'Content-Type: application/json' -d "$2")
  CORPO=$(cat "$TMP")
}

get() {
  CODIGO=$(curl -s -o "$TMP" -w '%{http_code}' "$API$1")
  CORPO=$(cat "$TMP")
}

# conferir <descricao> <esperado> <obtido>
conferir() {
  if [ "$2" = "$3" ]; then
    echo "  OK    $1"
    OK=$((OK + 1))
  else
    echo "  FALHA $1  (esperado: $2 | obtido: $3)"
    FALHAS=$((FALHAS + 1))
  fi
}

# contem <descricao> <trecho>  (confere o CORPO da ultima chamada)
contem() {
  if printf '%s' "$CORPO" | grep -q "$2"; then
    echo "  OK    $1"
    OK=$((OK + 1))
  else
    echo "  FALHA $1  (nao achei '$2' em: $CORPO)"
    FALHAS=$((FALHAS + 1))
  fi
}

corpo() { # corpo <titulo> <recompensa> <perguntas-json>
  printf '{"titulo":"%s","recompensaXp":%s,"perguntas":[%s]}' "$1" "$2" "$3"
}

id_do_corpo() {
  printf '%s' "$CORPO" | sed -n 's/.*"id" *: *"\([0-9a-f-]*\)".*/\1/p'
}

if [ "${1:-}" = "limpar" ]; then
  echo "Limpando o que o teste criou (titulos TESTE-161%)..."
  sql "DELETE FROM alternativa_quiz WHERE pergunta_id IN (SELECT p.id FROM pergunta_quiz p JOIN quiz q ON q.id = p.quiz_id WHERE q.titulo LIKE 'TESTE-161%')" >/dev/null
  sql "DELETE FROM pergunta_quiz WHERE quiz_id IN (SELECT id FROM quiz WHERE titulo LIKE 'TESTE-161%')" >/dev/null
  sql "DELETE FROM quiz WHERE titulo LIKE 'TESTE-161%'" >/dev/null
  echo "Feito. Se o clube 1 tinha um quiz ativo antes, ele continua inativo: para voltar ao seed de verdade use 'docker compose down -v' e suba de novo."
  exit 0
fi

echo "== 0. A API esta de pe? =="
# Logo depois do `docker compose up` o Spring ainda esta subindo (~15 s): espera ate 60 s.
for _ in $(seq 1 30); do
  get /actuator/health
  [ "$CODIGO" = "200" ] && break
  sleep 2
done
conferir "GET /actuator/health responde 200" "200" "$CODIGO"
if [ "$CODIGO" != "200" ]; then
  echo "A API nao respondeu em $API. Suba com: docker compose up --build -d"
  exit 1
fi
ANTES=$(sql "SELECT count(*) FROM quiz WHERE titulo LIKE 'TESTE-161%'")
if [ -z "$ANTES" ]; then
  echo "Nao consegui consultar o banco em '$DB_CONTAINER'. Confira o nome com: docker ps"
  exit 1
fi
[ "$ANTES" != "0" ] && echo "  AVISO: ja existem $ANTES quizzes TESTE-161 de uma rodada anterior. Rode './testar-quiz.sh limpar' antes."

echo
echo "== 1. O app sabe quem e lider (meuCargo) =="
get "/api/clubes/$CLUBE_LIDER";  contem "clube 1: meuCargo = administrador" '"meuCargo" *: *"administrador"'
get "/api/clubes/$CLUBE_MEMBRO"; contem "clube 3: meuCargo = membro" '"meuCargo" *: *"membro"'
get "/api/clubes/$CLUBE_FORA";   contem "clube 4: meuCargo = null" '"meuCargo" *: *null'

echo
echo "== 1b. O lider sabe se ha quiz ativo sem abrir o GET /quiz (revisao, item 3) =="
get "/api/clubes/$CLUBE_LIDER/quiz"
conferir "GET /quiz continua 403 para o administrador" "403" "$CODIGO"
get "/api/clubes/$CLUBE_MEMBRO"; contem "clube 3 (seed com quiz): temQuizAtivo = true" '"temQuizAtivo" *: *true'
ATIVO_CLUBE_1=$(sql "SELECT count(*) > 0 FROM quiz WHERE clube_id = '$CLUBE_LIDER' AND ativo")
get "/api/clubes/$CLUBE_LIDER"
if [ "$ATIVO_CLUBE_1" = "t" ]; then
  contem "clube 1: temQuizAtivo = true (ja tem quiz no banco)" '"temQuizAtivo" *: *true'
else
  contem "clube 1: temQuizAtivo = false (ainda sem quiz)" '"temQuizAtivo" *: *false'
fi

echo
echo "== 2. Quem nao e lider nao publica, e nada e gravado =="
post "$CLUBE_MEMBRO" "$(corpo 'TESTE-161 membro' 100 "$PERGUNTA")"
conferir "membro comum recebe 403" "403" "$CODIGO"; contem "codigo SO_LIDER_CRIA_QUIZ" 'SO_LIDER_CRIA_QUIZ'
post "$CLUBE_FORA" "$(corpo 'TESTE-161 fora' 100 "$PERGUNTA")"
conferir "quem nao participa recebe 403" "403" "$CODIGO"; contem "codigo SO_LIDER_CRIA_QUIZ" 'SO_LIDER_CRIA_QUIZ'
post "$CLUBE_INEXISTENTE" "$(corpo 'TESTE-161 sem clube' 100 "$PERGUNTA")"
conferir "clube inexistente recebe 404" "404" "$CODIGO"; contem "codigo CLUBE_NAO_ENCONTRADO" 'CLUBE_NAO_ENCONTRADO'

echo
echo "== 3. Corpo invalido responde 400 QUIZ_INVALIDO =="
invalido() { # invalido <descricao> <json>
  post "$CLUBE_LIDER" "$2"
  conferir "$1 -> 400" "400" "$CODIGO"
  contem "$1 -> QUIZ_INVALIDO" 'QUIZ_INVALIDO'
}
invalido "titulo vazio"            "$(corpo '' 100 "$PERGUNTA")"
invalido "titulo so com espacos"   "$(corpo '   ' 100 "$PERGUNTA")"
invalido "titulo com 121 letras"   "$(corpo "TESTE-161$(printf 'x%.0s' $(seq 1 112))" 100 "$PERGUNTA")"
invalido "recompensa 9"            "$(corpo 'TESTE-161 r9' 9 "$PERGUNTA")"
invalido "recompensa 501"          "$(corpo 'TESTE-161 r501' 501 "$PERGUNTA")"
invalido "nenhuma pergunta"        "$(corpo 'TESTE-161 sem perguntas' 100 '')"
invalido "alternativa vazia"       "$(corpo 'TESTE-161 alt vazia' 100 '{"enunciado":"?","alternativas":["A","","C","D"],"correta":0}')"
invalido "so 3 alternativas"       "$(corpo 'TESTE-161 tres alt' 100 '{"enunciado":"?","alternativas":["A","B","C"],"correta":0}')"
invalido "correta fora de 0..3"    "$(corpo 'TESTE-161 correta 4' 100 '{"enunciado":"?","alternativas":["A","B","C","D"],"correta":4}')"
invalido "enunciado vazio"         "$(corpo 'TESTE-161 enunciado' 100 '{"enunciado":"  ","alternativas":["A","B","C","D"],"correta":0}')"
conferir "nada foi gravado pelas tentativas recusadas" "0" "$(sql "SELECT count(*) FROM quiz WHERE titulo LIKE 'TESTE-161%'")"

echo
echo "== 4. O lider publica o quiz inteiro =="
DUAS='{"enunciado":"Primeira?","alternativas":["a1","a2","a3","a4"],"correta":0},{"enunciado":"Segunda?","alternativas":["b1","b2","b3","b4"],"correta":3}'
post "$CLUBE_LIDER" "$(corpo 'TESTE-161 primeiro' 100 "$DUAS")"
conferir "publicar responde 201" "201" "$CODIGO"
contem "totalPerguntas = 2" '"totalPerguntas" *: *2'
PRIMEIRO="$(id_do_corpo)"
echo "         id do quiz: $PRIMEIRO"
conferir "2 perguntas gravadas" "2" "$(sql "SELECT count(*) FROM pergunta_quiz WHERE quiz_id = '$PRIMEIRO'")"
conferir "8 alternativas gravadas" "8" "$(sql "SELECT count(*) FROM alternativa_quiz a JOIN pergunta_quiz p ON p.id = a.pergunta_id WHERE p.quiz_id = '$PRIMEIRO'")"
conferir "1 correta por pergunta (2 no total)" "2" "$(sql "SELECT count(*) FROM alternativa_quiz a JOIN pergunta_quiz p ON p.id = a.pergunta_id WHERE p.quiz_id = '$PRIMEIRO' AND a.correta")"
conferir "corretas sao A e D, na ordem das perguntas" "A,D" "$(sql "SELECT string_agg(a.letra, ',' ORDER BY p.ordem) FROM alternativa_quiz a JOIN pergunta_quiz p ON p.id = a.pergunta_id WHERE p.quiz_id = '$PRIMEIRO' AND a.correta")"
conferir "o quiz e do livro atual do clube" "t" "$(sql "SELECT q.livro_id = c.livro_id FROM quiz q JOIN clube_do_livro c ON c.id = q.clube_id WHERE q.id = '$PRIMEIRO'")"
conferir "o quiz ativo e o recem publicado" "t" "$(sql "SELECT ativo FROM quiz WHERE id = '$PRIMEIRO'")"
get "/api/clubes/$CLUBE_LIDER"
contem "depois de publicar, temQuizAtivo = true" '"temQuizAtivo" *: *true'

echo
echo "== 5. Publicar de novo substitui o quiz ativo =="
post "$CLUBE_LIDER" "$(corpo 'TESTE-161 segundo' 250 "$PERGUNTA")"
conferir "segunda publicacao responde 201" "201" "$CODIGO"
SEGUNDO="$(id_do_corpo)"
conferir "so 1 quiz ativo no clube" "1" "$(sql "SELECT count(*) FROM quiz WHERE clube_id = '$CLUBE_LIDER' AND ativo")"
conferir "o novo e o ativo" "t" "$(sql "SELECT ativo FROM quiz WHERE id = '$SEGUNDO'")"
conferir "o antigo ficou inativo" "f" "$(sql "SELECT ativo FROM quiz WHERE id = '$PRIMEIRO'")"
conferir "o antigo continua no banco, com as perguntas" "2" "$(sql "SELECT count(*) FROM pergunta_quiz WHERE quiz_id = '$PRIMEIRO'")"
conferir "o ativo do clube agora e o segundo" "TESTE-161segundo" "$(sql "SELECT titulo FROM quiz WHERE clube_id = '$CLUBE_LIDER' AND ativo")"
conferir "recompensa 250 gravada" "250" "$(sql "SELECT recompensa_xp FROM quiz WHERE id = '$SEGUNDO'")"

echo
echo "== 6. Limites aceitos =="
post "$CLUBE_LIDER" "$(corpo 'TESTE-161 xp10' 10 "$PERGUNTA")"; conferir "recompensa 10 aceita" "201" "$CODIGO"
post "$CLUBE_LIDER" "$(corpo 'TESTE-161 xp500' 500 "$PERGUNTA")"; conferir "recompensa 500 aceita" "201" "$CODIGO"
post "$CLUBE_LIDER" "$(corpo "TESTE-161$(printf 'x%.0s' $(seq 1 111))" 100 "$PERGUNTA")"; conferir "titulo de 120 letras aceito" "201" "$CODIGO"
conferir "continua so 1 ativo" "1" "$(sql "SELECT count(*) FROM quiz WHERE clube_id = '$CLUBE_LIDER' AND ativo")"

echo
echo "== 7. Correcoes do app pedidas na revisao (leitura do codigo) =="
RAIZ="$(cd "$(dirname "$0")" && pwd)"
CONFIGURAR="$RAIZ/mobile/src/features/clube/quiz/ConfigurarQuizScreen.tsx"
ADICIONAR="$RAIZ/mobile/src/features/clube/quiz/AdicionarPerguntasScreen.tsx"
PAGINA="$RAIZ/mobile/src/features/clube/PaginaClubeScreen.tsx"
CODIGO_ERRO="$RAIZ/backend/src/main/java/com/readrace/api/exception/CodigoErro.java"
tem() { # tem <descricao> <arquivo> <trecho>
  if grep -q "$3" "$2"; then echo "  OK    $1"; OK=$((OK + 1)); else echo "  FALHA $1"; FALHAS=$((FALHAS + 1)); fi
}
nao_tem() { # nao_tem <descricao> <arquivo> <trecho>
  if grep -q "$3" "$2"; then echo "  FALHA $1"; FALHAS=$((FALHAS + 1)); else echo "  OK    $1"; OK=$((OK + 1)); fi
}
tem     "item 1: Configurar Quiz rola (ScrollView)"              "$CONFIGURAR" '<ScrollView'
tem     "item 1: Configurar Quiz trata o teclado"                 "$CONFIGURAR" '<KeyboardAvoidingView'
tem     "item 1: Adicionar Perguntas rola (ScrollView)"           "$ADICIONAR"  '<ScrollView'
tem     "item 1: Adicionar Perguntas trata o teclado"             "$ADICIONAR"  '<KeyboardAvoidingView'
conferir "item 2: CLUBE_NAO_ENCONTRADO aparece uma vez so" "1" "$(grep -c 'CLUBE_NAO_ENCONTRADO(' "$CODIGO_ERRO")"
nao_tem "item 3: Adicionar Perguntas nao usa mais o GET /quiz"    "$ADICIONAR"  'buscarQuiz'
tem     "item 3: aviso de substituicao vem de temQuizAtivo"       "$ADICIONAR"  '{temQuizAtivo &&'
tem     "item 4: Acessar Quiz leva o lider para /quiz/configurar" "$PAGINA"     'quiz/configurar'
tem     "item 5: trava do Finalizar Quiz usa useRef"              "$ADICIONAR"  'publicacaoEmAndamento.current = true'

echo
echo "================================================"
echo "  Passaram: $OK   Falharam: $FALHAS"
echo "================================================"
echo "Para apagar o que foi criado: ./testar-quiz.sh limpar"
echo
echo "Falta conferir no celular (cd mobile && npx expo start), logado como lider do clube 1:"
echo "  1. Na pagina do clube, 'Acessar Quiz' abre 'Configurar Quiz' (nao mostra 'em desenvolvimento')."
echo "  2. 'Configurar Quiz' rola ate o botao 'Proximo' e as marcas do slider, acima da barra de abas."
echo "  3. Em 'Adicionar Perguntas', com 3+ perguntas, a tela rola ate 'Finalizar Quiz'; o teclado nao cobre os campos."
echo "  4. Como o clube ja tem quiz (este script publicou), aparece o aviso 'Publicar este quiz substitui...'."
echo "  5. Dois toques rapidos em 'Finalizar Quiz' publicam UM quiz so:"
echo "     docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c \"SELECT titulo, criado_em FROM quiz ORDER BY criado_em DESC LIMIT 3\""
[ "$FALHAS" -eq 0 ]
