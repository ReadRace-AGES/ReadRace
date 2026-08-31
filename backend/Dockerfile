# ============================================================
# Stage 1: BUILD
# Usa uma imagem com JDK + Maven para compilar o projeto
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copia o Maven Wrapper e o pom.xml primeiro (cache de dependências)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Baixa todas as dependências sem compilar o código-fonte.
# Essa camada é cacheada — só roda novamente se o pom.xml mudar.
RUN ./mvnw dependency:go-offline -B

# Agora copia o código-fonte
COPY src/ src/

# Compila o projeto gerando o .jar, pulando testes (rodam em outro estágio/CI)
RUN ./mvnw package -DskipTests -B

# ============================================================
# Stage 2: RUNTIME
# Usa apenas o JRE (menor e mais seguro) para rodar o .jar
# ============================================================
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# Cria um usuário não-root para rodar a aplicação (boa prática de segurança)
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Copia o .jar compilado do estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Define o usuário não-root
USER appuser

# Porta que o Spring Boot escuta
EXPOSE 8080

# Healthcheck interno do container.
# Forma shell de propósito: com a forma exec (["..."]) o "|| exit 1" faz o
# Docker desistir do parse JSON e passar o array inteiro como texto para o
# shell — o healthcheck nunca passa e o container fica sempre "unhealthy".
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Ponto de entrada com otimizações de memória para containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
