# ReadRace

Aplicativo mobile de incentivo ao hábito de leitura por meio de gamificação casual. Projeto acadêmico — PUCRS.

---

## Stack

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Mobile | React Native + Expo | Expo SDK 57, RN 0.86.2 |
| Backend | Java + Spring Boot | Java 21, Spring Boot 4.1 |
| Banco de Dados | PostgreSQL | 16 (alpine) |
| Build (back) | Maven | via wrapper (mvnw) |
| Build (mobile) | npm | Node 22 |
| Migrations | Flyway | - |
| Formatação | Spotless (Google Java Format AOSP) | - |
| CI/CD | GitHub Actions | backend-ci + mobile-ci |
| Documentacao API | Springdoc OpenAPI (Swagger UI) | 2.8.8 |
| Containers | Docker + Docker Compose | - |

---

## Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e Docker Compose (v2+)
- [Node.js 22](https://nodejs.org/) (para o mobile) — Download direto: [Windows](https://nodejs.org/dist/v22.23.2/node-v22.23.2-x64.msi) | [macOS](https://nodejs.org/dist/v22.23.2/node-v22.23.2.pkg) | [Linux](https://nodejs.org/dist/v22.23.2/node-v22.23.2-linux-x64.tar.xz)
- **OU**, para rodar o backend sem Docker:
  - Java 21 (JDK)
  - PostgreSQL 16 rodando localmente

---

## Inicio rapido

```bash
# 1. Clone o repositório e entre na branch de desenvolvimento
git clone <url-do-repositorio>
cd ReadRace
git checkout dev

# 2. Copie o arquivo de variáveis de ambiente
cp .env.example .env

# 3. Suba banco + API com um comando
docker compose up --build

# 4. Em outro terminal, suba o mobile
cd mobile
npm install
npm start
```

- API disponivel em `http://localhost:8080`
- Health check em `http://localhost:8080/actuator/health`
- Expo DevTools abre no navegador (escaneie o QR com Expo Go no celular)

---

## Backend — Formas de compilacao e execucao

### Opcao 1 — Tudo via Docker (recomendado para onboarding)

Nao precisa de Java nem PostgreSQL instalados. O Docker cuida de tudo.

```bash
# Subir banco + API
docker compose up --build

# Subir em background (detached)
docker compose up --build -d

# Ver logs
docker compose logs -f

# Parar tudo
docker compose down

# Parar e apagar dados do banco (volume)
docker compose down -v
```

**Rebuild apos mudanca no `pom.xml` ou no codigo:**
```bash
docker compose up --build api
```

---

### Opcao 2 — Banco no Docker, API local (recomendado para desenvolvimento)

Ideal para o dia a dia porque o hot reload (`spring-boot-devtools`) e instantaneo.

```bash
# 1. Sobe apenas o banco
docker compose up db -d

# 2. Roda a API localmente (requer Java 21)
cd backend
./mvnw spring-boot:run
```

No Windows:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

---

### Opcao 3 — Tudo local (sem Docker)

Se por algum motivo nao puder usar Docker:

1. Instale PostgreSQL 16 e crie o banco:
```sql
CREATE DATABASE readrace;
CREATE USER readrace WITH PASSWORD 'readrace';
GRANT ALL PRIVILEGES ON DATABASE readrace TO readrace;
```

2. Ajuste as variaveis no `.env` ou exporte:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/readrace
export DB_USER=readrace
export DB_PASSWORD=readrace
```

3. Compile e rode:
```bash
cd backend
./mvnw spring-boot:run
```

---

### Compilar o .jar sem executar

```bash
cd backend
./mvnw clean package -DskipTests
```

O artefato sera gerado em `backend/target/api-0.0.1-SNAPSHOT.jar`.

Para executar o .jar diretamente:
```bash
java -jar target/api-0.0.1-SNAPSHOT.jar
```

---

## Mobile — Como rodar

### Problema comum no Windows: script bloqueado pelo PowerShell

Se ao rodar `npm install` ou `npm start` aparecer o erro:

```
O arquivo D:\NodeJS\npm.ps1 não pode ser carregado porque a execução de scripts foi desabilitada neste sistema.
```

Execute este comando no PowerShell **como administrador**:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

Feche e abra o terminal novamente. Depois disso o npm funciona normalmente.

---

### Comandos

```bash
cd mobile
npm install
npm start
```

Opcoes apos o start:
- Pressione `s` para mudar para **Expo Go** (recomendado se aparecer erro de versao no celular)
- Pressione `a` para abrir no emulador Android
- Pressione `i` para abrir no simulador iOS (macOS)
- Pressione `w` para abrir no navegador (web)
- Escaneie o QR code com o app **Expo Go** no celular (mesmo WiFi)

> **Nota**: se ao escanear o QR code aparecer erro de versao incompativel no celular, pressione `s` no terminal do Expo para alternar o modo para Expo Go. Isso resolve o conflito de versao entre o SDK e o app.

### Scripts disponiveis

| Script | Comando | Descricao |
|--------|---------|-----------|
| start | `npm start` | Inicia o Expo DevTools |
| android | `npm run android` | Abre direto no emulador Android |
| ios | `npm run ios` | Abre direto no simulador iOS |
| web | `npm run web` | Abre no navegador |
| lint | `npm run lint` | Roda o linter do Expo |

---

## Executando testes

### Backend

A suite e dividida em dois grupos, e o sufixo do arquivo decide qual roda:

| Sufixo | Ferramenta | O que e | Precisa de Docker |
|--------|-----------|---------|-------------------|
| `*Test` | Surefire | Teste unitario, sem contexto Spring | Nao |
| `*IT` | Failsafe | Teste de integracao, contexto completo + PostgreSQL real via Testcontainers | Sim |

```bash
cd backend

# So os unitarios - rapido, nao precisa de Docker
./mvnw test

# Tudo: unitarios + integracao + formatacao + cobertura. E o que o CI roda.
./mvnw verify
```

O relatorio de cobertura sai em `backend/target/site/jacoco/index.html`. O build falha
se a cobertura de linha cair abaixo do piso definido em `jacoco.linha.minima` no `pom.xml`.

### Mobile

```bash
cd mobile
npx tsc --noEmit   # verificacao de tipos
```

---

## Formatacao de codigo (Spotless)

O backend usa Spotless com Google Java Format (estilo AOSP) para manter codigo consistente.

```bash
cd backend

# Verificar se esta formatado (o CI roda isso)
./mvnw spotless:check

# Corrigir automaticamente
./mvnw spotless:apply
```

> Dica: rode `./mvnw spotless:apply` antes de cada commit para evitar falhas no CI.

---

## CI/CD (GitHub Actions)

O projeto possui dois workflows que rodam automaticamente em push/PR para `main` e `dev`:

| Workflow | Arquivo | O que faz |
|----------|---------|-----------|
| Backend CI | `.github/workflows/backend-ci.yml` | `spotless:check` + `verify` (compila + testa) |
| Mobile CI | `.github/workflows/mobile-ci.yml` | `tsc --noEmit` (verificacao de tipos) |

Ambos usam path filter — so rodam se houver mudancas na pasta relevante.

---

## Variaveis de ambiente

| Variavel | Default | Descricao |
|----------|---------|-----------|
| `DB_NAME` | readrace | Nome do banco de dados |
| `DB_USER` | readrace | Usuario do banco |
| `DB_PASSWORD` | readrace | Senha do banco |
| `DB_PORT` | 5433 | Porta exposta do PostgreSQL no host |
| `DB_URL` | jdbc:postgresql://localhost:5433/readrace | URL JDBC (usado em execucao local) |
| `API_PORT` | 8080 | Porta exposta da API no host |
| `SERVER_PORT` | 8080 | Porta interna do Spring Boot |
| `SPRING_PROFILES_ACTIVE` | dev | Profile ativo do Spring |
| `CORS_ALLOWED_ORIGINS` | *(vazio)* | Origens liberadas no CORS, separadas por virgula. Vazio bloqueia tudo; o profile `dev` libera `*` |

---

## Estrutura do projeto

```
ReadRace/
├── .github/workflows/        # CI/CD (GitHub Actions)
│   ├── backend-ci.yml
│   └── mobile-ci.yml
├── .env.example              # Template de variaveis de ambiente
├── docker-compose.yml        # Orquestracao banco + API
├── README.md
├── backend/
│   ├── Dockerfile            # Build multi-stage (JDK -> JRE)
│   ├── .dockerignore
│   ├── pom.xml               # Dependencias Maven + Spotless
│   ├── mvnw / mvnw.cmd      # Maven Wrapper
│   └── src/
│       ├── main/
│       │   ├── java/com/readrace/api/
│       │   │   ├── ApiApplication.java
│       │   │   ├── config/                 # CORS, OpenAPI
│       │   │   ├── controller/
│       │   │   ├── dto/request/ + dto/response/
│       │   │   ├── exception/
│       │   │   ├── model/
│       │   │   ├── repository/
│       │   │   └── service/
│       │   └── resources/
│       │       ├── application.yaml        # comum a todos os ambientes
│       │       ├── application-dev.yaml    # log de SQL e CORS aberto - so em dev
│       │       └── db/migration/
│       └── test/                           # *Test unitario, *IT integracao
├── mobile/
│   ├── app.config.js         # Configuracao do Expo (le cores de src/theme/tokens)
│   ├── tailwind.config.js    # Deriva as classes de src/theme/tokens
│   ├── package.json
│   ├── tsconfig.json         # TypeScript strict + path aliases
│   ├── assets/               # Icones, splash, imagens
│   └── src/
│       ├── app/              # Rotas (file-based routing)
│       ├── components/       # Componentes reutilizaveis
│       ├── theme/            # Tokens de design - fonte unica de cor e medida
│       └── hooks/            # Custom hooks
```

---

## Tema do app

`mobile/src/theme/tokens.js` e a **fonte unica** de cor, tipografia, espacamento, raio, sombra e
tamanho. `tailwind.config.js` e `app.config.js` derivam dele, entao trocar um valor la muda o app
inteiro - classes Tailwind, estilos de `StyleSheet` e ate a cor da splash.

A tela `identidadevisual` mostra todos os tokens renderizados; e por ela que se confere uma mudanca.

### ⚠️ O Figma tem dois lotes de telas

Os frames com **`node-id >= 2011`** foram feitos as pressas e **nao servem para extrair valor de
design** - so como referencia de layout e fluxo:

`2011-531` `2011-674` `2011-1204` `2011-1290` `2011-1454` `2011-1538` `2043-609` `2054-657`
`2059-755` `2060-1726` `2060-1814` `2060-1910` - mais as copias `2082-*` e `2083-*`.

Os frames **abaixo de 2011** sao do designer e sao a unica fonte valida. Todo valor da tabela
abaixo saiu de um deles.

### Cores

| Token | Valor | Frame de origem |
|---|---|---|
| `primary` | `#732634` | `navbar` 34-322, cabecalho de `menu - comunidades` 3-7, botoes de 3-6 |
| `primarySoft` | `#823E4A` | `navbar` 34-322 |
| `accent` | `#C9425B` | coracao e "Ver todos" em `menu - social` 3-4, `card` 403-404 |
| `surface` | `#FEFEFE` | titulo e campo em 3-7 e `menu - adicionar livro` 271-279 |
| `surfaceMuted` | `#F5F5F5` | fundo do `card` 403-404 |
| `surfaceAlt` | `#E4E4E4` | linha alternada do ranking, `menu - clube do livro` 3-6 |
| `surfacePink` | `#F6E2E2` | bloco do post em `card` 403-404 - e `rgba(255,56,60,.1)` composto sobre `#F5F5F5` |
| `surfacePinkStrong` | `#EFC7CF` | chip de conquista em `perfil` 270-230 |
| `surfaceDisabled` | `#EDEDED` | conquista bloqueada em `perfil` 270-230 |
| `overlay` | `rgba(0,0,0,0.65)` | camada sob o modal de `menu - adicionar livro` 271-279 |
| `chipOnPrimary` | `rgba(255,255,255,0.11)` | chip "7 dias" sobre o cabecalho, 3-7 e 3-4 |
| `border` | `#EEEEEE` | borda do `card` 403-404 |
| `borderStrong` | `#D1D1D6` | separadores em `perfil` 270-230 |
| `inputBorder` | `#CBD5E1` | campo de texto em `menu - adicionar livro` 271-279 |
| `text` | `#1E1E1E` | corpo do `card` 403-404 |
| `textSecondary` | `#888888` | "3h atras" em `card` 403-404 |
| `textMuted` | `#A3A3A3` | conquista bloqueada em `perfil` 270-230 |
| `textInverse` | `#FEFEFE` | titulo e subtitulo do cabecalho, 3-7 e 3-8 |
| `navInactive*` | `#D98B9A` → `#AE7E86` | gradiente do icone inativo em `navbar` 34-322 |
| `progressTrack` | `#D9D9D9` | trilha da barra em `desafios` 3-8 |
| `rankGold/Silver/Bronze*` | ver `tokens.js` | 1o/2o/3o lugar em `menu - clube do livro` 3-6 |

### Tipografia

Familia **Inter**, nos pesos 400, 600, 700 e **800** (o titulo do cabecalho vermelho e ExtraBold).

| Item | Valor | Origem |
|---|---|---|
| Escala | 32 / 24 / 20 / 18 / 16 / 14 / 12 / 10 | `card` 403-404, `desafios` 3-8, `menu - comunidades` 3-7 |
| `lineHeight.normal` | `1.5` | 16px ocupa 24px, 14px ocupa 21px, 12px ocupa 18px (`card` 403-404) |
| `lineHeight.heading` | `0.9` | titulo do cabecalho: 21.4px em fonte de 24px (3-7 e 3-8) |
| `letterSpacingRatio` | `-0.011em` | `-0.176px`@16, `-0.154px`@14, `-0.132px`@12 (`card` 403-404) |

### Medidas

| Token | Valor | Origem |
|---|---|---|
| `buttonHeight` | `40` | `menu - comunidades` 3-7, `desafios` 3-8, `menu - adicionar livro` 271-279 |
| `inputHeight` | `40` | campo em `menu - adicionar livro` 271-279 |
| `navHeight` | `76` | instancia de `navbar` em 3-6, 3-7, 3-8 |
| `headerHeight` | `192` | cabecalho vermelho em 3-7 e 3-8 |
| `progressTrackHeight` | `8` | barra em `desafios` 3-8 |
| `chipHeight` | `23` | chips de XP e dificuldade em `desafios` 3-8 |
| `icon` / `iconSmall` | `24` / `16` | 3-6, 3-7, 3-8 |
| `avatar` / `avatarLarge` | `40` / `50` | 39-42px em 3-6/3-8/403-404; 50px na lista de 3-7 |
| `radius.xl` | `32` | cantos inferiores do cabecalho vermelho, 3-7 e 3-8 |
| `radius.pill` | `9999` | botoes e barra de progresso |

### Reconciliacoes deliberadas

O design nao esta numa grade regular. Onde valores proximos significam a mesma coisa, o tema
escolhe um so - e a decisao fica aqui em vez de ser rediscutida a cada componente:

- **Espacamento** normalizado em multiplos de 4. O design usa 7, 10, 11, 15 e 23px em pontos
  isolados (`card` 403-404, `menu - clube do livro` 3-6); o tema arredonda para 8, 12, 16 e 24.
- **Altura do botao = 40.** Tres frames medem 40 (3-7, 3-8, 271-279) e um mede 47 (3-6, que
  tambem usa botoes mais estreitos: 288px contra 325px dos demais). 40 e a maioria.
- **Raio do campo de texto.** O Figma mede 6px em 271-279; o tema usa `radius.sm` (8) para nao
  criar um degrau de 2px na escala.
- **Avatar em dois tamanhos, nao um.** 39, 42 e 50px aparecem no design; 39 e 42 viram `avatar`
  (40) e 50 vira `avatarLarge`, porque a diferenca de 10px e intencional.

> **Sem valor inventado.** `success` e `danger` foram removidos do tema: eram o Material Green 500
> e o Red 600, que nao aparecem em nenhum frame. Quando o design cobrir estados de sucesso e erro,
> o valor entra medido.

---

## Documentacao da API (Swagger)

Com a API rodando, acesse:

- **Swagger UI**: http://localhost:8080/swagger-ui.html (interface interativa)
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs (spec raw)

Todos os endpoints, DTOs e validacoes sao documentados automaticamente.

## Endpoints da API

| Metodo | URL | Status | Descricao |
|--------|-----|--------|-----------|
| GET | /actuator/health | 200 | Status da aplicacao |
| GET | /actuator/info | 200 | Informacoes da aplicacao |
| GET | /swagger-ui.html | 200 | Documentacao interativa (Swagger) |
| GET | /api/exemplos | 200 | Lista todos (boilerplate) |
| GET | /api/exemplos/{id} | 200/404 | Busca por ID |
| POST | /api/exemplos | 201/400 | Cria novo |
| PUT | /api/exemplos/{id} | 200/400/404 | Atualiza |
| DELETE | /api/exemplos/{id} | 204/404 | Exclui |

> Os endpoints `/api/exemplos` sao um boilerplate de referencia e serao substituidos pelas entidades reais do dominio.

---

## Convencoes do projeto

- **Branch principal de desenvolvimento**: `dev`
- **Commits**: portugues, seguindo Conventional Commits (`feat:`, `fix:`, `build:`, `chore:`, `ci:`, `docs:`)
- **Migrations Flyway**: `V<numero>__descricao_em_snake_case.sql` — imutaveis apos merge em dev
- **Hibernate**: modo `validate` — Flyway e dono do schema
- **Formatacao**: Spotless com Google Java Format (AOSP, 4 espacos)
- **Arquitetura backend**: Controller -> Service -> Repository (controller nao fala com repository)
- **DTOs**: records Java — Request (entrada com validacao), Response (saida com factory method)
- **Excecoes**: tratadas globalmente via `@RestControllerAdvice` com `ProblemDetail` (RFC 9457)
- **Spring Security**: desativado temporariamente ate implementacao do modulo de autenticacao
- **CORS**: fechado por padrao. Quem libera origem e a configuracao do ambiente, nunca o codigo -
  o profile `dev` libera `*`, os demais usam `CORS_ALLOWED_ORIGINS`
- **Profiles**: `application.yaml` guarda o que vale em todo ambiente; o que e so de desenvolvimento
  (log de SQL, CORS aberto) fica em `application-dev.yaml`. `./mvnw spring-boot:run` e o Docker
  Compose ja ativam `dev`
- **Cobertura**: piso verificado pelo JaCoCo no `verify`, definido em `jacoco.linha.minima`
- **Mobile routing**: expo-router (file-based, telas em `src/app/`)
- **Valores de design**: vem **so** de `mobile/src/theme/tokens.js`. Nenhuma cor, tamanho de fonte,
  espacamento ou raio escrito direto no componente. Se faltar um token, abra issue no tema - **nao
  meca o Figma**: parte das telas foi feita as pressas e nao serve de fonte (veja *Tema do app*).
  O Figma e referencia visual; o tema e a fonte de verdade
