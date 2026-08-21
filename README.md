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
<<<<<<< Updated upstream
- [Node.js 22](https://nodejs.org/) (para o mobile)
=======
- [Node.js 22](https://nodejs.org/) (para o mobile) — Download direto: [Windows](https://nodejs.org/dist/v22.23.2/node-v22.23.2-x64.msi) | [macOS](https://nodejs.org/dist/v22.23.2/node-v22.23.2.pkg) | [Linux](https://nodejs.org/dist/v22.23.2/node-v22.23.2-linux-x64.tar.xz)
>>>>>>> Stashed changes
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

```bash
cd mobile
npm install
npm start
```

Opcoes apos o start:
- Pressione `a` para abrir no emulador Android
- Pressione `i` para abrir no simulador iOS (macOS)
- Pressione `w` para abrir no navegador (web)
- Escaneie o QR code com o app **Expo Go** no celular (mesmo WiFi)

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

Os testes de integracao usam Testcontainers (sobe um PostgreSQL efemero via Docker automaticamente).

```bash
cd backend
./mvnw test
```

> Requisito: Docker precisa estar rodando para os testes de integracao funcionarem.

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
│       │       ├── application.yaml
│       │       └── db/migration/
│       └── test/
├── mobile/
│   ├── app.json              # Configuracao do Expo
│   ├── package.json
│   ├── tsconfig.json         # TypeScript strict + path aliases
│   ├── assets/               # Icones, splash, imagens
│   └── src/
│       ├── app/              # Rotas (file-based routing)
│       ├── components/       # Componentes reutilizaveis
│       ├── constants/        # Tema, cores, espacamentos
│       └── hooks/            # Custom hooks
```

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
- **CORS**: configurado para aceitar todas as origens em dev (restringir em prod)
- **Mobile routing**: expo-router (file-based, telas em `src/app/`)
