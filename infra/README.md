# Infraestrutura

Produção do backend na AWS, região **us-east-2 (Ohio)**. O mobile não passa por aqui.

```
GitHub Actions ──(OIDC)──> ECR (imagem da API)
      │
      └──(SSM: documento readrace-deploy)──> EC2 t3.small
                                               ├── caddy  :80 :443  (HTTPS, Let's Encrypt)
                                               ├── api    (Spring, perfil prod)
                                               └── db     (Postgres 16, sem porta exposta)
```

Endereço da API: `https://3-19-247-112.sslip.io` (sem domínio comprado; o
[sslip.io](https://sslip.io) resolve o IP escrito no nome).

## Pastas

| Caminho | Conteúdo |
|---|---|
| `infra/terraform/` | Tudo da AWS que é gerenciado por código |
| `infra/producao/` | `docker-compose.yml`, `Caddyfile` e `subir.sh` que rodam na EC2 |
| `.github/workflows/deploy.yml` | Deploy automático a cada push na `main` |

## O que existe na AWS

Gerenciado pelo Terraform: EC2 (importada do console), security group (80 e 443;
sem 22), Elastic IP, role e instance profile da EC2, ECR `readrace-api`, provedor
OIDC do GitHub, role `readrace-deploy-github` e documento SSM `readrace-deploy`.

Fora do Terraform, de propósito:

| Recurso | Por quê |
|---|---|
| Bucket `readrace-terraform-state-<conta>` | Guarda o state do próprio Terraform; criado uma vez pelo CLI |
| Parâmetros do Parameter Store | Se o Terraform os gerenciasse, o valor real iria para o state |
| Cognito (user pool e app client) | Ainda não importado |

### Segredos (Parameter Store, SecureString)

| Nome | Uso |
|---|---|
| `/readrace/prod/db/senha` | Senha do Postgres |
| `/readrace/prod/google-books/api-key` | Chave da Google Books API, restrita à Books API e ao IP da EC2 |

Os valores são criados e trocados pelo console da AWS. Não vão para o git, para o
Terraform nem para arquivos na EC2: o `subir.sh` lê a cada execução.

## Orçamento

Teto de **US$ 28,27/mês**. Estimativa real com tudo ligado o mês inteiro, preço sob
demanda: ~US$ 24,85 (EC2 t3.small 15,18; disco 20 GB 1,60; IPv4 3,65; ECR, S3 e
CloudWatch ~4,40). A `t3.medium` sozinha passaria do teto. Antes de criar qualquer
recurso pago, recalcular. Elastic IP sem instância associada continua sendo cobrado.

## Terraform

Pré-requisitos: Terraform ≥ 1.11, AWS CLI v2 e login com `aws login --region us-east-2`
(o usuário da AWS é bloqueado fora de us-east-2).

```bash
cd infra/terraform
cp backend.hcl.example backend.hcl   # preencher; backend.hcl não vai para o git
terraform init -backend-config=backend.hcl
AWS_REGION=us-east-2 terraform plan
AWS_REGION=us-east-2 terraform apply
```

Antes de aplicar, ler a última linha do plan. `destroy` diferente de zero ou
`must be replaced` em qualquer recurso: parar e entender. EC2, security group e
Elastic IP têm `prevent_destroy`.

## Deploy automático

Roda a cada push na `main` ou em **Actions → Deploy → Run workflow** (só na `main`):
build da imagem `linux/amd64`, envio ao ECR com a tag do commit (12 caracteres),
disparo do documento SSM e checagem de `/actuator/health`.

Depende da variável do repositório **`AWS_DEPLOY_ROLE_ARN`** (Settings → Secrets and
variables → Actions → Variables). Não é segredo. Se sumir, o valor sai de
`terraform output role_deploy_arn`.

A role só aceita execuções da `main` deste repositório, e só consegue disparar o
documento `readrace-deploy` nesta instância; não roda comando livre na EC2.

## Deploy manual

Para subir um commit fora do fluxo automático:

1. Build e envio da imagem (em Mac com Apple Silicon, ver a nota abaixo):
   ```bash
   REG=<conta>.dkr.ecr.us-east-2.amazonaws.com
   TAG=$(git rev-parse --short=12 HEAD)
   aws ecr get-login-password --region us-east-2 | docker login -u AWS --password-stdin $REG
   docker build --platform linux/amd64 -t $REG/readrace-api:$TAG backend
   docker push $REG/readrace-api:$TAG
   ```
2. Disparar o mesmo documento do deploy automático (o commit precisa estar no GitHub):
   ```bash
   aws ssm send-command --region us-east-2 --instance-ids i-0a27ef464e5619692 \
     --document-name readrace-deploy --parameters Commit=$(git rev-parse HEAD)
   ```

**Mac com Apple Silicon:** o build emulado para `linux/amd64` falha no `tar` do
Maven ("Function not implemented"). Compilar na arquitetura nativa e só a imagem
final em amd64 resolve: `FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build`
no `backend/Dockerfile`. No GitHub Actions o problema não existe.

## Operação

Acesso à máquina: **EC2 → instância ReadRace → Conectar → Session Manager**. Não
há SSH.

```bash
sudo docker ps                       # estado dos containers
sudo docker logs -f readrace-api-1   # logs da API
```

`docker compose ps` e `docker compose logs` falham fora do `subir.sh`: o compose
exige as variáveis de segredo e se recusa a ler o arquivo sem elas.

## Limitações conhecidas

- Disco da EC2 sem criptografia. Ligar exige um volume novo.
- A política `AmazonSSMManagedInstanceCore` já dá à EC2 leitura de qualquer
  parâmetro da conta; a restrição a `/readrace/prod/*` na role não limita na prática.
- Sem login: qualquer pessoa usa a API como o usuário do seed (V4) até o Cognito
  entrar. O Swagger está público.
- O nome sslip.io depende de um serviço de terceiros.
- Sem backup do banco, logs no CloudWatch nem alarmes ainda.
