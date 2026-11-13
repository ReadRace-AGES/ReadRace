# Plano de testes de ponta a ponta — ReadRace

Planejamento de 11/11/2026. Não instala dependências nem implementa testes.

## Base da análise

Repositório consultado pelo MCP GitHub: [ReadRace-AGES/ReadRace](https://github.com/ReadRace-AGES/ReadRace). Foram recuperadas as descrições das 132 issues encontradas nas quatro sprints, incluindo abertas e fechadas, sem filtrar por estado: sprint 1 com 39, sprint 2 com 28, sprint 3 com 30 e sprint 4 com 35. A união das buscas gerais em ordem crescente/decrescente foi comparada com as buscas por labels; nenhuma das issues recuperadas ficou sem sprint. A consulta por sprint-5 não retornou issues. A classificação usa as labels atuais, inclusive para tarefas transferidas de sprint.

A busca do conector não preencheu o estado individual das issues. Portanto, esta matriz representa requisitos, não um relatório de entregas concluídas. A #233 foi consultada individualmente e estava aberta, sem comentários. O package.json da dev foi conferido via MCP; arquivos locais da branch dev complementaram a análise da arquitetura. Não houve execução da aplicação ou da suíte nesta atividade.

Referência principal: [#233 — Playwright e apresentação](https://github.com/ReadRace-AGES/ReadRace/issues/233). Ela já especifica exportação web, API real em seed, PostgreSQL novo, viewport 390 × 844, nove fluxos e gravação. Este plano usa essa entrega como primeira etapa e amplia a regressão para as quatro sprints.

## Decisão de arquitetura

Usar @playwright/test em TypeScript dentro de mobile, executando o app exportado pelo Expo contra Spring Boot e PostgreSQL reais. O projeto já tem react-native-web, script web e armazenamento de sessão específico para navegador; não foi encontrada configuração Playwright. O CI mobile atual executa typecheck e npm test; o backend usa Maven, testes de integração e JaCoCo.

Playwright cobre a versão web compartilhada com o app. Emulação de tela de celular não comprova o comportamento do APK. Instalação, retorno do OAuth Google, SecureStore nativo, teclado, gestos e botão voltar do Android continuam no roteiro da [#226](https://github.com/ReadRace-AGES/ReadRace/issues/226). iOS e carga não fazem parte da #233.

Separar quatro execuções:

| Execução proposta | Ambiente | Finalidade |
|---|---|---|
| smoke-seed | Web exportada + API seed + banco descartável | Os nove caminhos felizes da #233 e recuperação do Feed |
| regression-seed | Mesmo ambiente, fixtures específicas | Regras de negócio, navegação, erros e persistência de todas as sprints |
| auth | Web com autenticação + API com validação de token | Sessão, isolamento de usuários e permissões; provedor controlado em CI e verificação real em homologação |
| presentation | Build próprio com Cognito + conta demo | Vídeos e capturas de um subconjunto adequado à apresentação |

Conservar os testes atuais. Concorrência, transações, migrações, cálculos e constraints continuam principalmente nos testes Java/integração. Playwright confirma os efeitos percebidos pelo usuário e pode usar sua fixture request para preparar dados e verificar contratos. Uma tela inteiramente mockada não conta como E2E integrado.

## Requisitos históricos que precisam ser reconciliados

Não converter literalmente todas as expectativas antigas em testes atuais:

| Expectativa histórica | Expectativa que orienta a regressão atual |
|---|---|
| #33 não altera nível, sequência, desafios ou pontos | #95–98, #163, #199, #203 e #205 introduzem a cascata e suas restrições |
| #37 cria desafio pendente | #97 define criação em andamento; não inventar fluxo de aceite |
| #41 exibe toast ao pedir revanche | #146 implementa revanche |
| #22 só exibe curtidas | #101 permite curtir/descurtir, com restrições da #224 |
| #99/#104 mostram ações provisórias | #155, #164–165, #201 e #221 implementam destinos reais |
| #147 mostra toast em Em alta | #238 implementa ordenação por curtidas |
| #152 impede o único líder de sair | #237 transfere liderança para maior pontuação; empate por entrada mais antiga |
| #165 oferece configurações provisórias | #249 deixa somente Desconectar e fechar |
| #98 conta livros marcados como lido | #199 exige leitura efetiva para a contagem de conquistas |
| #133 descreve completar perfil; #158 sugere vínculo por e-mail | #156/#224 e o provisionamento atual precisam orientar os testes; identidade já vinculada não pode ser apropriada por outra |
| #179 descreve autenticação desligada por padrão | Código atual usa Cognito por padrão; seed exige configuração explícita |

Cada divergência restante entre contrato e código deve virar registro na matriz e, se for defeito, uma issue associada à #240. Não ajustar expectativas apenas para reproduzir um bug. Exemplo a resolver antes de fixar a asserção de API: #33 especifica 422 para página inválida e #95 menciona 400.

## Implementação em etapas

### Etapa 1 — Ambiente reproduzível e prova de execução

1. Adicionar @playwright/test como devDependency, fixar a resolução no package-lock e instalar Chromium. Reutilizar Node 22 e JDK 21 dos workflows existentes, verificando compatibilidade na instalação.
2. Criar playwright.config.ts, scripts e um servidor estático com versão fixada. Exportar com `npx expo export --platform web` e servir dist na porta 4173. Validar as rotas geradas pelo Expo: o projeto usa web.output=static e rotas dinâmicas, portanto a exportação é um critério de saída desta etapa.
3. No build seed, definir EXPO_PUBLIC_API_URL para a API de teste e ambas EXPO_PUBLIC_COGNITO_DOMAIN e EXPO_PUBLIC_COGNITO_CLIENT_ID como strings vazias. Omiti-las não basta, pois há defaults. Variáveis EXPO_PUBLIC são resolvidas no build: trocar URL ou modo de autenticação exige outra exportação.
4. Subir PostgreSQL 16 descartável e API com AUTH_MODO=seed, Flyway e catálogo local. Criar composição própria de E2E, sem reutilizar o volume pgdata de desenvolvimento nem os container_name fixos do Compose atual. Identificar banco, containers e portas por execução.
5. Aguardar readiness de banco, /actuator/health e servidor web, com prazo e diagnóstico explícitos. Validar uma chamada de produto e navegação pela UI.
6. Configurar viewport 390 × 844, Chromium, workers=1 inicialmente, forbidOnly no CI, trace no primeiro retry, screenshot na falha e vídeo retido na falha. Usar webServer para o servidor web; a infraestrutura pode ser iniciada por script/CI antes da suíte.

Saída: comando local único sobe o ambiente e valida Feed → Biblioteca → detalhe, com relatório e limpeza do ambiente mesmo quando falha.

### Etapa 2 — Dados determinísticos e isolamento

Banco novo por execução é necessário, mas insuficiente: leitura, quiz, saída de clube e edição de perfil contaminam testes posteriores. Criar mecanismo de restauração do estado inicial antes de cada teste mutável e de cada retry. Usar script de banco restrito à infraestrutura local/CI, sem endpoint de reset em produção. A restauração deve respeitar as migrations e todas as tabelas relacionadas; reiniciar a API quando necessário para eliminar estado em memória.

Inicialmente rodar os testes sequencialmente. Paralelizar somente após ter banco/API isolados por worker ou usuários e recursos realmente independentes. Em AUTH_MODO=seed, todos os requests usam a mesma identidade: dois browser contexts não simulam dois usuários diferentes.

Fixtures mínimas: usuário demo; leitor novo; relação unilateral e amizade recíproca; líder, membro e não membro; clube com quiz não respondido; clube com pedidos pendentes; empate para sucessão; comunidade seguida/não seguida; livro sem leitura, em progresso e concluído; usuário próximo de nível/conquista; desafios perto do limite e expirados.

Para fluxos com duas pessoas, usar autenticação controlada com identidades distintas. Alternativa transitória no seed: duas instâncias da API com USUARIO_SEED_ID distintos apontando para o mesmo banco, com contextos/builds configurados para cada instância. Nenhuma das alternativas substitui os testes de autorização com tokens.

Datas precisam ser controladas no servidor e no banco, não apenas no navegador. O projeto tem ClockConfig, mas também LocalDate.now e timestamps SQL. Normalizar os pontos necessários para fixtures temporais ou deixar limites de data nos testes Java com relógio controlado. Respeitar America/Sao_Paulo, usado pela regra de sequência, e os períodos definidos pelo domínio. Não confundir esse fuso com o do operador do teste.

O seed pode começar com o semanal já em 70/70 (#205); preparar um cenário abaixo da meta para testar o pagamento. Não usar o seed geral como prova de travessia do limite.

### Etapa 3 — Nove testes obrigatórios da #233

| Arquivo sugerido | Caminho e resultado verificável |
|---|---|
| feed.spec.ts | Feed → Comunidades; grupos, comunidades e sequência carregados |
| biblioteca.spec.ts | Listas → adicionar pelo catálogo → detalhe → registrar página; percentual e persistência atualizados |
| busca.spec.ts | Três abas → perfil → seguir/desseguir → seguidores; contadores e retorno corretos |
| comunidade.spec.ts | Comunidade → curtir → comentar → responder; persistência e contadores |
| clube.spec.ts | Clube → ranking completo → fórum/publicação → chat/envio → quiz como membro; pontuação atualizada |
| criar-clube.spec.ts | Nome + livro do catálogo + dois membros → clube no Feed e ranking |
| desafios.spec.ts | Semanal e mensal → desafio por páginas com amigo recíproco → card criado |
| perfil.spec.ts | Conquistas → mascote disponível → editar nome; atualização ao retornar |
| erro.spec.ts | Interceptar apenas o carregamento do Feed com erro → mensagem e Tentar de novo → retirar falha → conteúdo real |

Cada teste possui seus próprios dados. Usar test.step para as etapas; nenhum teste depende da conclusão de outro. Navegar pelos controles do produto, sem abrir URLs internas para pular o fluxo. Conferir o resultado após sair e voltar ou recarregar, além da confirmação imediata.

### Etapa 4 — Regressão completa por domínio

| Domínio / issues | Cenários adicionais obrigatórios |
|---|---|
| Navegação e componentes — #9, #11, #14–29, #102–107, #221–222 | Cinco abas; voltar preservando termo, resultados e aba de origem; overlays e telas ocultas; fechamento de painel; labels; carregando/desabilitado; toque repetido; 320 px sem corte crítico; imagens com fallback; toast sem esconder navegação |
| Biblioteca e catálogo — #30–32, #43–44, #48, #155, #218–219 | Todas as listas; busca vazia/sem resultados; adicionar e reutilizar livro por ISBN ou título+autor normalizados; autores diferentes; favoritar/desfavoritar; trocar lista; remover/cancelar; bloquear remoção com leitura; marcado Lido sem XP; estado refletido no perfil |
| Leitura e recompensas — #33, #95–98, #163, #199, #203 | Página absoluta; avançar, repetir, retroceder e retornar à máxima; página inválida; conclusão e +150 uma vez; nível, conquistas e sequência; aviso em detalhe e clube; pontos apenas nos clubes do mesmo livro; nenhuma recompensa sem páginas novas |
| Feed social — #197, #204 | Somente pessoas seguidas; excluir leitura própria; agregado por pessoa/livro/dia; não contar retrocesso; conclusão única; autor/livro navegáveis; retorno ao Feed; estado vazio com Buscar leitores |
| Desafios — #37–42, #97, #146, #198, #205 | Só amigos recíprocos; páginas e livro; escolher/trocar/remover livro; prazo e validações; vitória/derrota/empate; revanche; livro errado não avança; semanal 70 páginas e mensal 2 livros; +100/+200 uma vez por período; falha parcial preserva seção de amigos |
| Social e comunidades — #34, #101, #147, #150, #154, #201, #235–236, #238 | Seguir/desseguir e seguir de volta; listas de seguidores/seguidos; participar de sugeridas; publicar e comentar com permissões; respostas; curtida idempotente e rollback visual; Em alta por curtidas e recentes por data; texto preservado após erro; único administrador não abandona comunidade |
| Clubes — #35–36, #148–153, #200, #202, #234, #237 | Criar com livro e membros; ranking e perfil por posição; pedir entrada/aceitar/recusar; não membro sem ações administrativas; fórum e chat separados; receber mensagem de outro usuário; sair/cancelar; sucessão por pontos e desempate; último membro encerra clube |
| Quiz — #160–162, #224 | Líder configura; título, quatro alternativas e resposta correta; remover pergunta; limites de texto; publicação substitui quiz ativo; membro responde correto/errado; retomar na próxima; concluir; pontos e XP separados; resposta repetida sem prêmio; não membro e membro comum sem poderes de líder |
| Perfil — #45, #99–100, #164–165, #201, #239, #249 | Próprio versus outro perfil; conquistas bloqueadas/desbloqueadas; favoritos; estatísticas de leitura real; mascote bloqueado rejeitado; nome/username e conflito; e-mail não editável; avatar/fundo persistidos e avatar refletido no chat; configuração geral apenas desconectar e fechar |
| Autenticação — #131–135, #156–159, #179, #224–225 | Login válido/inválido; cadastro; senha inválida; confirmação e reenvio; conta não confirmada; primeiro acesso; conta nova sem seed alheio; restaurar sessão; renovar token uma vez sob chamadas concorrentes; refresh revogado; logout inclusive offline; token ausente/inválido/client errado; isolamento por identidade |
| Falhas transversais — #10, #222, #224, #255 | 400/401/403/404/409 e 5xx conforme contrato; queda de rede; mensagem segura; retry; preservar rascunho; impedir envio duplicado; erro não pode parecer lista vazia; não expor SQL; desconexão sem ERROR validada no backend |

Testes de regra de alto risco devem verificar a API real e refletir o resultado em tela. Para a leitura, um cenário de referência é máxima 100 → 145 (+45 de páginas) → 100 (0) → 145 (0) → 200 (+55) → fim (+restante e +150 uma vez). Isolar bônus de conquista/desafios nas fixtures: o total global pode ser maior que o XP das páginas.

A sequência não segue a suposição comum de zerar após um dia sem leitura: #96 permite pausa de até sete dias, e oito ou mais interrompem a sequência. #199 também impede que repetir a página prolongue a sequência. Cobrir esses limites em integração e um efeito representativo no navegador.

Para cada domínio, incluir sucesso, vazio, erro recuperável e persistência quando aplicáveis. Casos de limites e concorrência não precisam multiplicar jornadas longas de UI: associar testes de API/Java à mesma issue.

### Etapa 5 — Autenticação real e permissões

Manter a suíte seed rápida e sem dependência de Cognito. Acrescentar uma suíte separada em que o backend valida tokens: provedor/JWKS de teste com configuração isolada e tokens assinados para diferentes identidades, expiração e claims. Não desabilitar validação para afirmar cobertura de autorização.

Os estados de UI de login/cadastro podem usar respostas controladas do provedor no CI, identificadas como testes de integração de UI. Uma execução de homologação com user pool de teste cobre login real, confirmação de e-mail por caixa de testes, renovação e logout. Credenciais e limpeza das contas de teste são pré-requisitos dessa execução, não da suíte seed.

Reutilizar storageState para jornadas autenticadas que não testam o login. Não compartilhar a mesma conta mutável entre workers; não versionar .auth nem publicar tokens nos artefatos. Continuar validando OAuth Google e retorno ao APK pelo roteiro #226. O vínculo de identidades segue #224; não assumir que e-mail igual autoriza juntar perfis já vinculados.

### Etapa 6 — CI, evidências e apresentação

Criar `.github/workflows/e2e.yml` com Node 22, JDK 21, PostgreSQL 16, instalação travada, browsers, build da API, exportação web, readiness, testes e upload de artefatos mesmo em falha. Preservar workflows existentes. Acionar smoke em PRs para dev/main que alterem mobile, backend, migrations, fixtures ou configuração E2E; regressão completa em push para dev, agenda e workflow_dispatch. Se o tempo inicial for excessivo, a #233 permite começar em push/dev e manual, promovendo o smoke ao PR após estabilizar.

Propor scripts: `test:e2e`, `test:e2e:smoke`, `test:e2e:regression`, `test:e2e:ui`, `test:e2e:report` e `test:e2e:presentation`. Nomear tags @smoke, @regression, @auth e @presentation. Relatórios HTML/JUnit e trace devem identificar commit, cenário, issue e ambiente. Usar retenção definida para logs e imagens. Um teste que passa só no retry continua registrado como instável; retry não é critério de qualidade.

Configurar apresentação com video=on, capturas por etapa e espera por document.fonts.ready e imagens antes de capturar. O relatório de teste e os vídeos são entregas distintas: validar que os arquivos de vídeo foram finalizados ao fechar os contexts. Nomear a pasta com execução, fluxo e etapa.

Build de apresentação usa Cognito e API configurados explicitamente. A #233 permite produção com a conta demo da #225; não executar automaticamente a regressão destrutiva inteira ali. Definir um roteiro separado, com mutações intencionais conhecidas, sem reset do banco nem saídas de clube que destruam a demonstração. A preparação por #220 deve ser coordenada antes da gravação. Falhas de rede simuladas e casos destrutivos ficam no ambiente descartável.

A issue prevê vídeo e prints em 13/11/2026. Como este plano é de 11/11, a prioridade até essa data é ambiente + nove fluxos + gravação. Cobertura completa, autenticação multiusuário e limites temporais são uma segunda entrega; não prometer que todos cabem nesses dois dias.

## Organização sugerida

```text
mobile/playwright.config.ts
mobile/e2e/fixtures/{test,dados,sessao}.ts
mobile/e2e/pages/                 # helpers pequenos por tela ou componente
mobile/e2e/smoke/                 # nove testes da #233
mobile/e2e/regression/            # cenários por domínio
mobile/e2e/auth/                  # login, sessão e múltiplas identidades
mobile/e2e/presentation/          # roteiro e capturas
mobile/e2e/coverage.json          # issue → requisito atual → caso → camada → evidência
infra/e2e/                       # composição, seed, reset e encerramento
.github/workflows/e2e.yml
```

Preferir getByRole/getByLabel, complementando accessibilityRole/accessibilityLabel do React Native quando faltarem. Adicionar testID somente onde necessário. As telas da pilha permanecem no DOM com aria-hidden: limitar a busca à tela ativa, sem resolver ambiguidades com first() arbitrário. Usar locator.click(), auto-wait e assertions; evitar element.click(), sleeps fixos e seletores por classe. Estender o padrão já previsto na #107.

## Prioridade, esforço e critérios de conclusão

Estimativa inicial em dias de trabalho de uma pessoa familiarizada com o projeto, sujeita ao primeiro build e às lacunas encontradas:

| Pacote | Esforço indicativo | Dependência |
|---|---|---|
| Ambiente, exportação, fixtures e primeiro smoke | 1–2 dias | Nenhuma |
| Nove fluxos, CI e evidências | 2–3 dias | Ambiente reproduzível |
| Regressão de leitura, biblioteca, desafios e navegação | 2–3 dias | Reset confiável |
| Clubes, comunidade, quiz e perfil completos | 2–3 dias | Fixtures de papéis e dados |
| Autenticação e múltiplas identidades | 2–3 dias | Ambiente autenticado de teste |
| Apresentação e estabilização | 1–2 dias | Smoke e conta demo |

Total indicativo: 10–16 dias de trabalho, sem incluir correções relevantes no produto. Prioridade máxima: os nove fluxos e proteção contra duplicação de XP/pontos, acesso indevido e perda de dados. As estimativas não representam compromisso com a data da apresentação.

Entrega mínima concluída quando os nove testes passam localmente e no CI sobre banco novo; uma quebra controlada de expectativa produz falha e screenshot; vídeo e prints abrem corretamente; nenhuma credencial é versionada; a execução seguinte começa limpa.

Cobertura ampliada concluída quando todos os requisitos funcionais vigentes da matriz têm cenário e evidência; casos de API/integração ou APK têm camada e responsável indicados; nenhuma exclusão fica implícita; não há P0/P1 conhecido pendente; testes podem rodar isoladamente e repetir sobre estado limpo. Propor três execuções consecutivas sem falha intermitente como verificação inicial de estabilidade. Medir duração antes de definir orçamento de CI.

Não usar “100% de linhas” como sinônimo de todos os fluxos. coverage.json deve registrar issue, critério, substituição por requisito posterior, teste, camada, prioridade e estado (planejado/implementado/passou/falhou/bloqueado), com link para a execução. Épicos agregam a cobertura dos filhos; tarefas de infraestrutura e documentação têm validação própria.

## Fontes técnicas

- [Expo web](https://docs.expo.dev/workflow/web/): execução e exportação para navegador.
- [Playwright — configuração](https://playwright.dev/docs/test-configuration) e [webServer](https://playwright.dev/docs/test-webserver).
- [Playwright — boas práticas](https://playwright.dev/docs/best-practices): comportamento observável, locators e isolamento.
- [Playwright — autenticação](https://playwright.dev/docs/auth): storageState e contas separadas para testes mutáveis.
- [Playwright — CI](https://playwright.dev/docs/ci): instalação de browsers e execução automatizada.

## Inventário de todas as issues consultadas

A seguir, a rastreabilidade integral por sprint. A coluna de encaminhamento indica a camada planejada, não cobertura já implementada. Os requisitos detalhados e suas evoluções estão nos links de cada issue.


### Sprint 1 — 39 issues

| Issue | Requisito | Encaminhamento |
|---|---|---|
| [#9](https://github.com/ReadRace-AGES/ReadRace/issues/9) | Criar o projeto mobile com navegação de abas e stack | Navegação / componentes / erros |
| [#10](https://github.com/ReadRace-AGES/ReadRace/issues/10) | Criar o projeto backend com `CurrentUser` e o formato padrão de resposta e erro | Ambiente / API / migrações / CI |
| [#11](https://github.com/ReadRace-AGES/ReadRace/issues/11) | Extrair do Figma e registrar o tema do app | Navegação / componentes / erros |
| [#12](https://github.com/ReadRace-AGES/ReadRace/issues/12) | Modelar o schema do banco com todas as entidades do produto | Ambiente / API / migrações / CI |
| [#13](https://github.com/ReadRace-AGES/ReadRace/issues/13) | Popular o banco com o seed de todas as tabelas | Ambiente / API / migrações / CI |
| [#14](https://github.com/ReadRace-AGES/ReadRace/issues/14) | Criar o componente `BottomNav` com as 5 abas e estado ativo | Navegação / componentes / erros |
| [#15](https://github.com/ReadRace-AGES/ReadRace/issues/15) | Criar o componente `AppHeader` com as variantes de título, voltar e sequência | Navegação / componentes / erros |
| [#16](https://github.com/ReadRace-AGES/ReadRace/issues/16) | Criar o componente `PrimaryButton` com as variantes preenchida, contorno, com ícone, desabilitada e carregando | Navegação / componentes / erros |
| [#17](https://github.com/ReadRace-AGES/ReadRace/issues/17) | Criar o componente `Card` como container base das listas e blocos do app | Navegação / componentes / erros |
| [#18](https://github.com/ReadRace-AGES/ReadRace/issues/18) | Criar o componente `SegmentedTabs` de 2 e 3 opções | Navegação / componentes / erros |
| [#19](https://github.com/ReadRace-AGES/ReadRace/issues/19) | Criar o componente `BookCover` com tamanhos de grade e destaque | Navegação / componentes / erros |
| [#20](https://github.com/ReadRace-AGES/ReadRace/issues/20) | Criar o componente `Avatar` com placeholder de inicial e badge de sequência | Navegação / componentes / erros |
| [#21](https://github.com/ReadRace-AGES/ReadRace/issues/21) | Criar o componente `ProgressBar` com rótulo e percentual | Navegação / componentes / erros |
| [#22](https://github.com/ReadRace-AGES/ReadRace/issues/22) | Criar o componente `PostCard` com "Ler mais" e curtidas só de exibição | Navegação / componentes / erros |
| [#23](https://github.com/ReadRace-AGES/ReadRace/issues/23) | Criar o componente `ListItem` com capa, título, subtítulo, metadado e ação | Navegação / componentes / erros |
| [#24](https://github.com/ReadRace-AGES/ReadRace/issues/24) | Criar o componente `SearchInput`, campo de busca com ícone e placeholder parametrizável | Navegação / componentes / erros |
| [#25](https://github.com/ReadRace-AGES/ReadRace/issues/25) | Criar o componente `EmptyState`, padrão único de lista vazia para toda a sprint | Navegação / componentes / erros |
| [#26](https://github.com/ReadRace-AGES/ReadRace/issues/26) | Criar o componente `BottomSheet`, painel que sobe por baixo com fundo escurecido | Navegação / componentes / erros |
| [#27](https://github.com/ReadRace-AGES/ReadRace/issues/27) | Criar o componente `RankingRow`, linha do ranking de Pontos do Clube do Livro | Navegação / componentes / erros |
| [#28](https://github.com/ReadRace-AGES/ReadRace/issues/28) | Criar o componente `Slider`, seleção de valor numérico entre um mínimo e um máximo | Navegação / componentes / erros |
| [#29](https://github.com/ReadRace-AGES/ReadRace/issues/29) | Criar o componente `Toast` com o aviso "Funcionalidade em desenvolvimento" | Navegação / componentes / erros |
| [#30](https://github.com/ReadRace-AGES/ReadRace/issues/30) | Entregar a aba Meus Livros com favoritos, últimos livros e os 4 estados da biblioteca | Biblioteca / busca / catálogo |
| [#31](https://github.com/ReadRace-AGES/ReadRace/issues/31) | Expor a API de detalhe do livro com progresso do usuário e posts sobre o livro | Biblioteca / busca / catálogo |
| [#32](https://github.com/ReadRace-AGES/ReadRace/issues/32) | Construir a tela Detalhe do livro com progresso e posts sobre o livro | Biblioteca / busca / catálogo |
| [#33](https://github.com/ReadRace-AGES/ReadRace/issues/33) | Registrar em que página o usuário parou e calcular o XP da leitura | Leitura / gamificação / integração |
| [#34](https://github.com/ReadRace-AGES/ReadRace/issues/34) | Entregar o feed de comunidades com meus clubes e minhas comunidades | Feed / comunidades / social |
| [#35](https://github.com/ReadRace-AGES/ReadRace/issues/35) | Entregar a Página do clube com livro atual, ações e ranking por pontos | Clubes / fórum / chat / papéis |
| [#36](https://github.com/ReadRace-AGES/ReadRace/issues/36) | Entregar o Fórum do clube com a lista de posts da leitura atual | Clubes / fórum / chat / papéis |
| [#37](https://github.com/ReadRace-AGES/ReadRace/issues/37) | Criar a API de desafios entre amigos — listar, criar e acompanhar progresso | Desafios / regras temporais |
| [#43](https://github.com/ReadRace-AGES/ReadRace/issues/43) | Criar a API de busca de livros, usuários e comunidades | Biblioteca / busca / catálogo |
| [#44](https://github.com/ReadRace-AGES/ReadRace/issues/44) | Construir a tela Buscar com as três abas de resultado | Biblioteca / busca / catálogo |
| [#45](https://github.com/ReadRace-AGES/ReadRace/issues/45) | Criar a API e a tela de Perfil de outro usuário | Perfil / conquistas / personalização |
| [#48](https://github.com/ReadRace-AGES/ReadRace/issues/48) | Criar API para busca de livro | Biblioteca / busca / catálogo |
| [#51](https://github.com/ReadRace-AGES/ReadRace/issues/51) | Fundação — projeto, banco e tema | Agregador: cobertura pelos requisitos filhos |
| [#52](https://github.com/ReadRace-AGES/ReadRace/issues/52) | Componentes genéricos — a biblioteca de UI da sprint | Agregador: cobertura pelos requisitos filhos |
| [#53](https://github.com/ReadRace-AGES/ReadRace/issues/53) | Biblioteca — a aba Meus Livros | Agregador: cobertura pelos requisitos filhos |
| [#54](https://github.com/ReadRace-AGES/ReadRace/issues/54) | Detalhe do livro e progresso | Agregador: cobertura pelos requisitos filhos |
| [#55](https://github.com/ReadRace-AGES/ReadRace/issues/55) | Social — feed de comunidades, página do clube e fórum | Agregador: cobertura pelos requisitos filhos |
| [#57](https://github.com/ReadRace-AGES/ReadRace/issues/57) | Busca e perfil de outro usuário | Agregador: cobertura pelos requisitos filhos |


### Sprint 2 — 28 issues

| Issue | Requisito | Encaminhamento |
|---|---|---|
| [#38](https://github.com/ReadRace-AGES/ReadRace/issues/38) | Construir a tela Desafiar amigo com meta por páginas | Desafios / regras temporais |
| [#39](https://github.com/ReadRace-AGES/ReadRace/issues/39) | Adicionar a meta por livro específico à tela Desafiar | Desafios / regras temporais |
| [#40](https://github.com/ReadRace-AGES/ReadRace/issues/40) | Construir a tela Escolher Livro do desafio | Desafios / regras temporais |
| [#41](https://github.com/ReadRace-AGES/ReadRace/issues/41) | Exibir o resultado de um desafio finalizado | Desafios / regras temporais |
| [#42](https://github.com/ReadRace-AGES/ReadRace/issues/42) | Construir a tela raiz da aba Desafios com a seção "Desafio com amigos" | Desafios / regras temporais |
| [#56](https://github.com/ReadRace-AGES/ReadRace/issues/56) | Desafio entre amigos | Agregador: cobertura pelos requisitos filhos |
| [#95](https://github.com/ReadRace-AGES/ReadRace/issues/95) | Somar o XP da leitura ao total do usuário e recalcular o nível | Leitura / gamificação / integração |
| [#96](https://github.com/ReadRace-AGES/ReadRace/issues/96) | Atualizar a sequência de dias consecutivos ao registrar leitura | Leitura / gamificação / integração |
| [#97](https://github.com/ReadRace-AGES/ReadRace/issues/97) | Avançar o progresso do desafio ativo e resolver o confronto | Leitura / gamificação / integração |
| [#98](https://github.com/ReadRace-AGES/ReadRace/issues/98) | Reavaliar e desbloquear conquistas quando o usuário lê ou termina um desafio | Leitura / gamificação / integração |
| [#99](https://github.com/ReadRace-AGES/ReadRace/issues/99) | Entregar a aba Perfil com o perfil do próprio usuário | Perfil / conquistas / personalização |
| [#100](https://github.com/ReadRace-AGES/ReadRace/issues/100) | Construir a tela Minhas Conquistas com as desbloqueadas e as próximas | Perfil / conquistas / personalização |
| [#101](https://github.com/ReadRace-AGES/ReadRace/issues/101) | Fazer o coração dos posts curtir e descurtir de verdade | Feed / comunidades / social |
| [#102](https://github.com/ReadRace-AGES/ReadRace/issues/102) | Corrigir o voltar do Detalhe do livro quando se chega pela Busca | Navegação / componentes / erros |
| [#103](https://github.com/ReadRace-AGES/ReadRace/issues/103) | Tirar do app as rotas que não são produto | Navegação / componentes / erros |
| [#104](https://github.com/ReadRace-AGES/ReadRace/issues/104) | Trocar o aviso local de Meus Livros pelo componente de Toast | Navegação / componentes / erros |
| [#105](https://github.com/ReadRace-AGES/ReadRace/issues/105) | Fazer o CI do mobile rodar os testes, não só a verificação de tipos | Ambiente / API / migrações / CI |
| [#106](https://github.com/ReadRace-AGES/ReadRace/issues/106) | Corrigir as três capas do catálogo inicial que carregam em branco | Navegação / componentes / erros |
| [#107](https://github.com/ReadRace-AGES/ReadRace/issues/107) | Dar nome e papel de acessibilidade ao botão voltar do cabeçalho | Navegação / componentes / erros |
| [#108](https://github.com/ReadRace-AGES/ReadRace/issues/108) | Cascata do registro de leitura | Agregador: cobertura pelos requisitos filhos |
| [#109](https://github.com/ReadRace-AGES/ReadRace/issues/109) | Perfil e conquistas | Agregador: cobertura pelos requisitos filhos |
| [#110](https://github.com/ReadRace-AGES/ReadRace/issues/110) | Social interativo — parar de ser só leitura | Agregador: cobertura pelos requisitos filhos |
| [#111](https://github.com/ReadRace-AGES/ReadRace/issues/111) | Dívida da sprint 1 | Agregador: cobertura pelos requisitos filhos |
| [#131](https://github.com/ReadRace-AGES/ReadRace/issues/131) | Autenticação — login com e-mail e senha | Agregador: cobertura pelos requisitos filhos |
| [#132](https://github.com/ReadRace-AGES/ReadRace/issues/132) | Ligar o usuário ao Cognito e extrair a interface UsuarioAtual | Ambiente / API / migrações / CI |
| [#133](https://github.com/ReadRace-AGES/ReadRace/issues/133) | Validar o token do Cognito e criar o perfil no primeiro acesso | Autenticação / sessão / autorização |
| [#134](https://github.com/ReadRace-AGES/ReadRace/issues/134) | Trazer o Cognito para o Terraform e liberar o cadastro pelo app | Ambiente / API / migrações / CI |
| [#135](https://github.com/ReadRace-AGES/ReadRace/issues/135) | Sessão do app e token em toda chamada da API | Autenticação / sessão / autorização |


### Sprint 3 — 30 issues

| Issue | Requisito | Encaminhamento |
|---|---|---|
| [#146](https://github.com/ReadRace-AGES/ReadRace/issues/146) | Desafiar de novo o mesmo amigo pelo botão "Revanche" | Desafios / regras temporais |
| [#147](https://github.com/ReadRace-AGES/ReadRace/issues/147) | Abrir o perfil de uma comunidade e seguir ou deixar de seguir | Feed / comunidades / social |
| [#148](https://github.com/ReadRace-AGES/ReadRace/issues/148) | Criar um clube do livro com o livro do catálogo e os primeiros membros | Clubes / fórum / chat / papéis |
| [#149](https://github.com/ReadRace-AGES/ReadRace/issues/149) | Escrever post no Fórum do clube | Clubes / fórum / chat / papéis |
| [#150](https://github.com/ReadRace-AGES/ReadRace/issues/150) | Ler e escrever comentários num post da comunidade | Feed / comunidades / social |
| [#151](https://github.com/ReadRace-AGES/ReadRace/issues/151) | Conversar com os membros do clube no chat do clube | Clubes / fórum / chat / papéis |
| [#152](https://github.com/ReadRace-AGES/ReadRace/issues/152) | Sair de um clube do livro | Clubes / fórum / chat / papéis |
| [#153](https://github.com/ReadRace-AGES/ReadRace/issues/153) | Mostrar o ranking completo do clube no "Ver todos" | Clubes / fórum / chat / papéis |
| [#154](https://github.com/ReadRace-AGES/ReadRace/issues/154) | Seguir e deixar de seguir outro leitor pelo perfil dele | Feed / comunidades / social |
| [#155](https://github.com/ReadRace-AGES/ReadRace/issues/155) | Adicionar um livro à biblioteca buscando no catálogo e escolhendo a lista | Biblioteca / busca / catálogo |
| [#156](https://github.com/ReadRace-AGES/ReadRace/issues/156) | Entrar com e-mail e senha na tela de login do app | Autenticação / sessão / autorização |
| [#157](https://github.com/ReadRace-AGES/ReadRace/issues/157) | Criar conta dentro do app, com o código de confirmação por e-mail | Autenticação / sessão / autorização |
| [#158](https://github.com/ReadRace-AGES/ReadRace/issues/158) | Entrar com a conta Google | Validação Android / OAuth real (#226) |
| [#159](https://github.com/ReadRace-AGES/ReadRace/issues/159) | Gerar o app instalável para Android e ligar o login em produção | Validação Android / OAuth real (#226) |
| [#160](https://github.com/ReadRace-AGES/ReadRace/issues/160) | Preparar o clube para o quiz: tabelas do quiz, quiz pronto no seed e o meu cargo no clube | Ambiente / API / migrações / CI |
| [#161](https://github.com/ReadRace-AGES/ReadRace/issues/161) | Deixar o líder do clube configurar o quiz e adicionar as perguntas | Quiz / papéis / recompensas |
| [#162](https://github.com/ReadRace-AGES/ReadRace/issues/162) | Deixar o membro responder o quiz do clube e ganhar pontos por acerto | Quiz / papéis / recompensas |
| [#163](https://github.com/ReadRace-AGES/ReadRace/issues/163) | Somar pontos no clube quando o membro registra leitura do livro do clube | Leitura / gamificação / integração |
| [#164](https://github.com/ReadRace-AGES/ReadRace/issues/164) | Escolher o mascote entre os desbloqueados pelo nível | Perfil / conquistas / personalização |
| [#165](https://github.com/ReadRace-AGES/ReadRace/issues/165) | Abrir as configurações pela engrenagem do perfil, editar nome e usuário, e desconectar | Perfil / conquistas / personalização |
| [#166](https://github.com/ReadRace-AGES/ReadRace/issues/166) | Quiz e pontos do clube — o ranking passa a andar | Agregador: cobertura pelos requisitos filhos |
| [#167](https://github.com/ReadRace-AGES/ReadRace/issues/167) | Clube vivo — criar, conversar, ver o ranking e sair | Agregador: cobertura pelos requisitos filhos |
| [#168](https://github.com/ReadRace-AGES/ReadRace/issues/168) | Comunidades e conteúdo — entrar numa comunidade | Agregador: cobertura pelos requisitos filhos |
| [#169](https://github.com/ReadRace-AGES/ReadRace/issues/169) | Biblioteca — colocar livro novo | Agregador: cobertura pelos requisitos filhos |
| [#170](https://github.com/ReadRace-AGES/ReadRace/issues/170) | Autenticação no app — login de verdade, no celular de quem usa | Agregador: cobertura pelos requisitos filhos |
| [#171](https://github.com/ReadRace-AGES/ReadRace/issues/171) | Personalização — mascotes e configurações | Agregador: cobertura pelos requisitos filhos |
| [#179](https://github.com/ReadRace-AGES/ReadRace/issues/179) | Documentar a configuração do login com Cognito para o time | Documentação / operação / revisão fora da suíte de UI |
| [#181](https://github.com/ReadRace-AGES/ReadRace/issues/181) | Monitorar a produção: alarmes e logs no CloudWatch | Documentação / operação / revisão fora da suíte de UI |
| [#184](https://github.com/ReadRace-AGES/ReadRace/issues/184) | Publicar as páginas de Gerência, Processo e Design na wiki do projeto | Documentação / operação / revisão fora da suíte de UI |
| [#185](https://github.com/ReadRace-AGES/ReadRace/issues/185) | Documentar o banco de dados na wiki do projeto | Documentação / operação / revisão fora da suíte de UI |


### Sprint 4 — 35 issues

| Issue | Requisito | Encaminhamento |
|---|---|---|
| [#195](https://github.com/ReadRace-AGES/ReadRace/issues/195) | Recompensa e social da leitura — ler aparece para mim e para quem me segue | Agregador: cobertura pelos requisitos filhos |
| [#196](https://github.com/ReadRace-AGES/ReadRace/issues/196) | Desafios do sistema — a meta do mês e da semana | Agregador: cobertura pelos requisitos filhos |
| [#197](https://github.com/ReadRace-AGES/ReadRace/issues/197) | Mostrar na aba Social quantas páginas as pessoas que eu sigo leram | Feed / comunidades / social |
| [#198](https://github.com/ReadRace-AGES/ReadRace/issues/198) | Mostrar o desafio do mês e o da semana na aba Desafios | Desafios / regras temporais |
| [#199](https://github.com/ReadRace-AGES/ReadRace/issues/199) | Fazer a leitura só dar recompensa quando a pessoa lê de verdade | Leitura / gamificação / integração |
| [#200](https://github.com/ReadRace-AGES/ReadRace/issues/200) | Colocar o usuário de demonstração no ranking do clube do quiz | Ambiente / API / migrações / CI |
| [#201](https://github.com/ReadRace-AGES/ReadRace/issues/201) | Ver quem segue e quem é seguido ao tocar nos números do perfil | Feed / comunidades / social |
| [#202](https://github.com/ReadRace-AGES/ReadRace/issues/202) | Abrir o perfil de um leitor tocando no ranking do clube | Clubes / fórum / chat / papéis |
| [#203](https://github.com/ReadRace-AGES/ReadRace/issues/203) | Mostrar o que ganhei logo depois de registrar leitura | Leitura / gamificação / integração |
| [#204](https://github.com/ReadRace-AGES/ReadRace/issues/204) | Mostrar "Terminou o livro" na aba Social e abrir o perfil e o livro pelo card | Feed / comunidades / social |
| [#205](https://github.com/ReadRace-AGES/ReadRace/issues/205) | Pagar o XP do desafio do mês e da semana quando a pessoa cumpre a meta | Desafios / regras temporais |
| [#218](https://github.com/ReadRace-AGES/ReadRace/issues/218) | Reusar o livro que já existe ao adicionar pelo catálogo | Biblioteca / busca / catálogo |
| [#219](https://github.com/ReadRace-AGES/ReadRace/issues/219) | Trocar a lista de um livro da biblioteca, favoritar e remover | Biblioteca / busca / catálogo |
| [#220](https://github.com/ReadRace-AGES/ReadRace/issues/220) | Deixar os dados de demonstração coerentes e criar o "refrescar a demo" | Ambiente / API / migrações / CI |
| [#221](https://github.com/ReadRace-AGES/ReadRace/issues/221) | Tirar os botões sem destino, acertar o voltar e o painel de registrar leitura | Navegação / componentes / erros |
| [#222](https://github.com/ReadRace-AGES/ReadRace/issues/222) | Acertar textos, mensagens de erro e telas pequenas | Navegação / componentes / erros |
| [#223](https://github.com/ReadRace-AGES/ReadRace/issues/223) | Limpar o que sobrou do setup e reforçar o CI antes da entrega | Ambiente / API / migrações / CI |
| [#224](https://github.com/ReadRace-AGES/ReadRace/issues/224) | Deixar a API pronta para contas de verdade | Autenticação / sessão / autorização |
| [#225](https://github.com/ReadRace-AGES/ReadRace/issues/225) | Deixar pronta a conta de demonstração, com os dados do seed, para testes e apresentação | Autenticação / sessão / autorização |
| [#226](https://github.com/ReadRace-AGES/ReadRace/issues/226) | Testar o app instalado e caçar bugs antes da entrega | Validação Android / OAuth real (#226) |
| [#227](https://github.com/ReadRace-AGES/ReadRace/issues/227) | Atualizar as páginas Arquitetura e Configuração da wiki com o que existe hoje | Documentação / operação / revisão fora da suíte de UI |
| [#228](https://github.com/ReadRace-AGES/ReadRace/issues/228) | Atualizar Escopo, Gerência, Processo e Design da wiki e registrar as diferenças entre o Figma e o app | Documentação / operação / revisão fora da suíte de UI |
| [#229](https://github.com/ReadRace-AGES/ReadRace/issues/229) | App sem ponta solta — tudo o que aparece funciona | Agregador: cobertura pelos requisitos filhos |
| [#230](https://github.com/ReadRace-AGES/ReadRace/issues/230) | Pronto para entregar — produção, dados, testes e documentação | Agregador: cobertura pelos requisitos filhos |
| [#233](https://github.com/ReadRace-AGES/ReadRace/issues/233) | Testar os fluxos do app de ponta a ponta com Playwright e gravar a apresentação | Apresentação / suíte transversal |
| [#234](https://github.com/ReadRace-AGES/ReadRace/issues/234) | Pedir para entrar num clube e o líder aprovar ou recusar | Clubes / fórum / chat / papéis |
| [#235](https://github.com/ReadRace-AGES/ReadRace/issues/235) | Escrever post na comunidade e participar das comunidades sugeridas | Feed / comunidades / social |
| [#236](https://github.com/ReadRace-AGES/ReadRace/issues/236) | Comentar nos posts do fórum do clube e do detalhe do livro | Feed / comunidades / social |
| [#237](https://github.com/ReadRace-AGES/ReadRace/issues/237) | Passar a liderança do clube quando o único líder sai | Clubes / fórum / chat / papéis |
| [#238](https://github.com/ReadRace-AGES/ReadRace/issues/238) | Mostrar os posts "Em alta" no perfil da comunidade | Feed / comunidades / social |
| [#239](https://github.com/ReadRace-AGES/ReadRace/issues/239) | Escolher o avatar e o plano de fundo do perfil | Perfil / conquistas / personalização |
| [#240](https://github.com/ReadRace-AGES/ReadRace/issues/240) | Bugs da entrega — tudo o que o teste achar, com dono e prazo | Agregador: cobertura pelos requisitos filhos |
| [#249](https://github.com/ReadRace-AGES/ReadRace/issues/249) | Deixar nas Configurações gerais só o que funciona | Perfil / conquistas / personalização |
| [#250](https://github.com/ReadRace-AGES/ReadRace/issues/250) | Desenhar no Figma as telas que o app ganhou sem design | Documentação / operação / revisão fora da suíte de UI |
| [#255](https://github.com/ReadRace-AGES/ReadRace/issues/255) | Não registrar como ERROR o cliente que desconecta antes da resposta | Integração backend / observabilidade |

