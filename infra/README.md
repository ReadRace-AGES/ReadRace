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
| `infra/producao/` | `docker-compose.yml`, `Caddyfile`, `subir.sh` e o backup do banco, que rodam na EC2 |
| `.github/workflows/deploy.yml` | Deploy automático a cada push na `main` |

## O que existe na AWS

Gerenciado pelo Terraform: EC2 (importada do console), security group (80 e 443;
sem 22), Elastic IP, role e instance profile da EC2, ECR `readrace-api`, provedor
OIDC do GitHub, role `readrace-deploy-github`, documento SSM `readrace-deploy`, bucket de backup
`readrace-backup-<conta>` e o Cognito (user pool, app client e domínio, importados do console).

Fora do Terraform, de propósito:

| Recurso | Por quê |
|---|---|
| Bucket `readrace-terraform-state-<conta>` | Guarda o state do próprio Terraform; criado uma vez pelo CLI |
| Parâmetros do Parameter Store | Se o Terraform os gerenciasse, o valor real iria para o state |

### Segredos (Parameter Store, SecureString)

| Nome | Uso |
|---|---|
| `/readrace/prod/db/senha` | Senha do Postgres |
| `/readrace/prod/google-books/api-key` | Chave da Google Books API, restrita à Books API e ao IP da EC2 |

Os valores são criados e trocados pelo console da AWS. Não vão para o git, para o
Terraform nem para arquivos na EC2: o `subir.sh` lê a cada execução.

## Cognito (login)

User pool `us-east-2_MEVIpejhy`, app client `5f06kagodbku0mvveakmsim8qu` (sem secret),
em `infra/terraform/cognito.tf`. Login por e-mail e senha pelo próprio app; a API só
valida o token (épico #131).

- Cadastro pelo próprio usuário, com código de confirmação por e-mail em português.
- Fluxo de login do app: `USER_PASSWORD_AUTH`. Token de acesso de 1 hora, renovação de
  30 dias, revogação ligada.
- Envio de e-mail pelo próprio Cognito: gratuito, cerca de **50 por dia**. Se passar disso
  (cadastros em massa numa apresentação, por exemplo), os códigos param de chegar até o
  dia seguinte; a saída é configurar o SES.
- `name` e `email` são atributos obrigatórios desde a criação do pool e **não podem
  mudar**. O Terraform ignora o `schema` de propósito: uma diferença ali faria o plan
  propor recriar o pool, o que apaga todas as contas.
- Política de senha: em aberto, decisão do time. Mudar é in-place.

### Ligar a autenticação na API

A API recebe a configuração pelo `infra/producao/docker-compose.yml`
(`COGNITO_ISSUER_URI`, `COGNITO_CLIENT_ID`, `COGNITO_DOMAIN`, valores públicos). O login
fica **desligado** (`AUTH_MODO` com padrão `seed`) até o app com login ser lançado. Para
ligar, trocar o padrão para `cognito` no compose, num PR, e fazer o deploy: em `seed`, a
API atende todo mundo como o usuário do seed.

O app entra pela página de login do Cognito (domínio `us-east-2mevipejhy`), que já traz
cadastro, código de confirmação e "esqueci a senha". O Google entra como botão nessa página
quando for configurado como provedor.

### Conta de demonstração

Liga o usuário fixo do seed (Daniel Ribeiro, com histórico, XP e conquistas) a uma conta
do Cognito, para as apresentações. Depende da coluna `cognito_sub` (#132). Usar um e-mail
do projeto: o código de confirmação e a recuperação de senha chegam nele.

```bash
# 1. criar o usuário com senha definitiva, sem e-mail de convite
aws cognito-idp admin-create-user --region us-east-2 --user-pool-id us-east-2_MEVIpejhy \
  --username <e-mail do projeto> --message-action SUPPRESS \
  --user-attributes Name=email,Value=<e-mail do projeto> Name=email_verified,Value=true Name="name",Value="Daniel Ribeiro"
aws cognito-idp admin-set-user-password --region us-east-2 --user-pool-id us-east-2_MEVIpejhy \
  --username <e-mail do projeto> --password '<senha>' --permanent

# 2. ler o sub
aws cognito-idp admin-get-user --region us-east-2 --user-pool-id us-east-2_MEVIpejhy \
  --username <e-mail do projeto> --query 'UserAttributes[?Name==`sub`].Value' --output text

# 3. ligar ao Daniel do seed (na EC2, pelo Session Manager)
sudo docker exec -i readrace-db-1 psql -U readrace -d readrace -c \
  "UPDATE usuario SET cognito_sub = '<sub>' WHERE id = '00000000-0000-0000-0000-000000000001';"
```

Nunca ligar contas por e-mail: os e-mails do seed (`@readrace.com`) são de um domínio real
de terceiros.

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

## Backup do banco

`pg_dump` do Postgres para o bucket `readrace-backup-<conta>`, em dois momentos:

| Prefixo | Quando |
|---|---|
| `diario/` | Todo dia às 06:00 UTC (03:00 em Brasília), pelo timer do systemd |
| `antes-do-deploy/` | No `subir.sh`, antes de trocar a imagem. Se falhar, o deploy não acontece |

Os arquivos expiram sozinhos depois de **30 dias**. A EC2 grava e lê no bucket, mas não
apaga. O `subir.sh` instala o timer a cada deploy.

```bash
systemctl list-timers readrace-backup      # próxima execução
journalctl -u readrace-backup              # resultado das últimas
sudo /opt/readrace/backup-banco.sh manual  # backup na hora
```

### Restaurar

Na EC2, pelo Session Manager. Primeiro num banco à parte, para conferir:

```bash
BUCKET=readrace-backup-$(aws sts get-caller-identity --region us-east-2 --query Account --output text)
aws s3 ls s3://$BUCKET/ --recursive --region us-east-2 | sort | tail   # escolher o arquivo
aws s3 cp s3://$BUCKET/<chave> /var/tmp/restaurar.dump --region us-east-2

DB=$(sudo docker ps -q --filter label=com.docker.compose.service=db)
sudo docker exec $DB createdb -U readrace readrace_restaurado
sudo docker exec -i $DB pg_restore -U readrace -d readrace_restaurado --no-owner < /var/tmp/restaurar.dump
sudo docker exec $DB psql -U readrace -d readrace_restaurado -c "SELECT count(*) FROM usuario;"
```

Para substituir o banco de produção, parar a API antes
(`sudo docker stop readrace-api-1`), restaurar com `pg_restore --clean --if-exists`
em `readrace` e subir de novo com o `subir.sh` da tag que estava no ar. No fim,
apagar `/var/tmp/restaurar.dump` e o banco `readrace_restaurado` (`dropdb`).

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
- Sem logs no CloudWatch nem alarmes ainda: uma falha do backup diário só aparece
  no `journalctl`.
