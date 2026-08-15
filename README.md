# ReadRace

Aplicativo mobile de incentivo ao hábito de leitura por meio de gamificação casual. Projeto acadêmico — PUCRS.

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Mobile | React Native |
| Backend | Java 21 + Spring Boot 4.1 |
| Banco de Dados | PostgreSQL 16 |
| Build | Maven (wrapper incluso) |
| Migrations | Flyway |
| Containers | Docker + Docker Compose |

---

## Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e Docker Compose (v2+)
- **OU**, para rodar sem Docker:
  - Java 21 (JDK)
  - PostgreSQL 16 rodando localmente

---

## Início rápido

```bash
# 1. Clone o repositório e entre na branch de desenvolvimento
git clone <url-do-repositorio>
cd ReadRace
git checkout dev

# 2. Copie o arquivo de variáveis de ambiente
cp .env.example .env

# 3. Suba tudo (banco + API) com um comando
docker compose up --build
```

A API estará disponível em `http://localhost:8080` e o health check em `http://localhost:8080/actuator/health`.

---

## Formas de compilação e execução

### Opção 1 — Tudo via Docker (recomendado para onboarding)

Não precisa de Java nem PostgreSQL instalados. O Docker cuida de tudo.

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

**Rebuild após mudança no `pom.xml` ou no código:**
```bash
docker compose up --build api
```

---

### Opção 2 — Banco no Docker, API local (recomendado para desenvolvimento)

Ideal para o dia a dia porque o hot reload (`spring-boot-devtools`) é instantâneo.

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

### Opção 3 — Tudo local (sem Docker)

Se por algum motivo não puder usar Docker:

1. Instale PostgreSQL 16 e crie o banco:
```sql
CREATE DATABASE readrace;
CREATE USER readrace WITH PASSWORD 'readrace';
GRANT ALL PRIVILEGES ON DATABASE readrace TO readrace;
```

2. Ajuste as variáveis no `.env` ou exporte:
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

O artefato será gerado em `backend/target/api-0.0.1-SNAPSHOT.jar`.

Para executar o .jar diretamente:
```bash
java -jar target/api-0.0.1-SNAPSHOT.jar
```

---

## Executando testes

Os testes de integração usam Testcontainers (sobe um PostgreSQL efêmero via Docker automaticamente).

```bash
cd backend
./mvnw test
```

> Requisito: Docker precisa estar rodando para os testes de integração funcionarem.

---

## Variáveis de ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `DB_NAME` | readrace | Nome do banco de dados |
| `DB_USER` | readrace | Usuário do banco |
| `DB_PASSWORD` | readrace | Senha do banco |
| `DB_PORT` | 5433 | Porta exposta do PostgreSQL no host |
| `DB_URL` | jdbc:postgresql://localhost:5433/readrace | URL JDBC (usado em execução local) |
| `API_PORT` | 8080 | Porta exposta da API no host |
| `SERVER_PORT` | 8080 | Porta interna do Spring Boot |
| `SPRING_PROFILES_ACTIVE` | dev | Profile ativo do Spring |

---

## Estrutura do projeto

```
ReadRace/
├── .env.example              # Template de variáveis de ambiente
├── docker-compose.yml        # Orquestração banco + API
├── backend/
│   ├── Dockerfile            # Build multi-stage (JDK → JRE)
│   ├── .dockerignore
│   ├── pom.xml               # Dependências Maven
│   ├── mvnw / mvnw.cmd      # Maven Wrapper
│   └── src/
│       ├── main/
│       │   ├── java/com/readrace/api/
│       │   │   └── ApiApplication.java
│       │   └── resources/
│       │       └── application.yaml
│       └── test/
│           └── java/com/readrace/api/
│               ├── ApiApplicationTests.java
│               ├── TestApiApplication.java
│               └── TestcontainersConfiguration.java
```

---

## Endpoints úteis

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Status da aplicação e conexão com o banco |
| `GET /actuator/info` | Informações da aplicação |

---

## Convenções do projeto

- **Branch principal de desenvolvimento**: `dev`
- **Migrations**: Flyway em `src/main/resources/db/migration/` com nomes `V1__descricao.sql`
- **Hibernate**: modo `validate` — o Flyway é dono do schema, o Hibernate apenas valida
- **Spring Security**: desativado temporariamente até implementação do módulo de autenticação
