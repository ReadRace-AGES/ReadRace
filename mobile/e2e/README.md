# Testes Playwright

Implementação inicial do [plano das quatro sprints](../../docs/plano-playwright.md): nove testes de smoke da #233, treze regressões de UI/API e um roteiro separado de apresentação. Os testes integrados usam o app Expo exportado, Spring Boot e PostgreSQL reais. Somente os cenários explícitos de falha interceptam chamadas.

## Executar

Na pasta `mobile`, com Node 22 e Docker Compose disponíveis:

```sh
npm ci
npx playwright install chromium
npm run test:e2e:smoke
npm run test:e2e:regression
npm run test:e2e
npm run test:e2e:report
```

`test:e2e` cria um projeto Docker exclusivo, compila API e web, aplica migrations, salva o estado inicial, restaura o banco antes de **cada teste e retry**, executa Chromium a 390 × 844 e remove o ambiente ao terminar. A API usa a porta 18080 e a web 4173. Para outra execução simultânea, definir `E2E_API_PORT` e `E2E_WEB_PORT` diferentes. A restauração desliga e religa a API para descartar conexões e caches. Não utiliza o volume de desenvolvimento nem permite selecionar uma API externa no modo seed.

O primeiro build pode demorar para baixar Maven, imagens e browsers. O CI instala também as dependências Linux do Chromium. PRs executam smoke; push para dev e execução manual rodam todos os testes.

```sh
npm run test:e2e -- --grep 'Perfil:' --retries=0
npm run test:e2e:ui
npx playwright test --list
```

O comando direto `playwright test` serve para listar testes. Para executar, usar o runner: ele fornece o isolamento exigido pelas fixtures. No PowerShell, colocar filtros começando com `@` entre aspas.

## Alternativa sem Docker

Com JDK 21 e PostgreSQL local instalado, o modo nativo cria **outro cluster temporário**, sem utilizar o serviço/banco existente:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
$env:E2E_PG_BIN = 'C:/Program Files/PostgreSQL/17/bin'
npm run test:e2e -- --native
```

Os caminhos são exemplos e devem corresponder à instalação local. A porta do cluster é 15432, configurável por `E2E_DB_PORT`. O diretório temporário, a API e o cluster são removidos/encerrados ao terminar. O reset usa pg_restore transacional entre testes; o JDBC desse ambiente desativa prepared statements persistentes para evitar planos com tipos antigos após restaurar o schema. O CI usa PostgreSQL 16 via Docker; a validação local pode usar outra versão instalada.

## Evidências

- `playwright-report/`: relatório HTML.
- `test-results/`: screenshot e vídeo em falha; trace no primeiro retry.
- `e2e-artifacts/`: JUnit, identificação da execução e logs dos serviços.

Esses arquivos são ignorados pelo Git. O workflow publica evidências por sete dias, inclusive quando os testes falham. Nenhuma execução local altera issues ou publica artefatos remotamente.

## Apresentação

Definir `E2E_API_URL` (HTTPS), `E2E_EMAIL`, `E2E_PASSWORD`, `EXPO_PUBLIC_COGNITO_DOMAIN` e `EXPO_PUBLIC_COGNITO_CLIENT_ID` no ambiente, então executar:

```sh
npm run test:e2e:presentation
```

O runner gera outro build, com autenticação habilitada, faz login e percorre as cinco áreas principais, salvando vídeo e capturas. Esse roteiro não importa a fixture de reset, não altera livros/clubes/perfil e não roda automaticamente no CI. Credenciais nunca devem entrar em arquivos versionados. O vídeo inclui a tela de login: tratar os arquivos como privados. A execução real depende da conta demo e do Cognito; não equivale a uma suíte completa de autenticação.

A API e o client Cognito precisam aceitar a origem local usada pela apresentação; configurar CORS e callbacks no ambiente de homologação apropriado.

## Validação em 11/11/2026

Execução completa local com Chromium, API real, JDK 21 e PostgreSQL 17 temporário: **22 testes passaram, sem retries, em 48 segundos** (fora do tempo de build/startup). Também passaram `npm test`, `tsc --noEmit`, Prettier e a exportação web. Os tipos de rotas locais do Expo foram regenerados antes do typecheck.

O Docker Desktop local não respondeu; o caminho Docker/CI foi preparado, mas não executado nesta validação. O roteiro de apresentação autenticada não foi executado por depender de credenciais e configuração do Cognito. Screenshots, vídeos e contexto de erro foram produzidos nas rodadas de diagnóstico; as falhas da suíte foram corrigidas antes da execução completa acima.

## Cobertura e limites

As referências `#issue` estão nos nomes dos testes. `coverage.json` relaciona todas as 132 issues do levantamento aos testes diretos, sem marcar os demais critérios como cobertos. Uma referência indica cobertura parcial daquela issue, não de todos os critérios.

Esta entrega cobre os nove caminhos principais, regras de XP/conclusão, entradas inválidas, remoção protegida, navegação da busca/feed, erros recuperáveis, comunidade sugerida/publicação, pedido de entrada, publicação de quiz, sucessão, mascote bloqueado e configurações gerais. Restam expansões do plano: autenticação multiusuário/Cognito, aprovação/recusa entre duas identidades, limites de semana/mês e demais combinações de erros/permissões. Os testes Java e Node existentes continuam responsáveis pelas regras que já verificam.

APK, Google OAuth nativo, SecureStore, teclado/gestos e voltar do Android seguem a #226. Os testes de navegador não comprovam esses comportamentos.

Ao acrescentar testes, navegar pela UI, escolher a tela ativa e limitar seletores ao modal quando necessário. Esperar a resposta e o resultado visível antes de consultar persistência; texto preenchido em um campo não prova que foi salvo. Não compartilhar dados mutáveis nem habilitar workers paralelos com a mesma identidade seed.
