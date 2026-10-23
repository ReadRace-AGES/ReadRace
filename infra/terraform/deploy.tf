# Deploy automático: o GitHub Actions entra na AWS por OIDC, sem nenhuma chave guardada
# no GitHub, envia a imagem ao ECR e manda a EC2 se atualizar.

# Faz a AWS confiar nos tokens que o GitHub emite para cada execução de workflow.
# Cada token vale minutos e diz de qual repositório e branch a execução veio.
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

locals {
  repositorio_github = "ReadRace-AGES/ReadRace"
  # O repositório usa o "sub" imutável do GitHub, que leva o ID numérico da organização
  # e do repositório junto do nome. Um repositório apagado e recriado com o mesmo nome
  # ganha IDs novos e não herda o acesso. Conferir em
  # gh api repos/ReadRace-AGES/ReadRace/actions/oidc/customization/sub
  sub_github = "repo:ReadRace-AGES@317697324/ReadRace@1334676753"
}

# Só um workflow rodando na main deste repositório assume a role. Fork, outra branch
# ou outro repositório recebem o token do GitHub, mas a AWS recusa.
data "aws_iam_policy_document" "deploy_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.sub_github}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "deploy" {
  name                 = "readrace-deploy-github"
  assume_role_policy   = data.aws_iam_policy_document.deploy_assume.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "deploy" {
  statement {
    sid       = "LoginNoEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EnviarImagemDaApi"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
      "ecr:BatchGetImage",
      "ecr:DescribeImages",
    ]
    resources = [aws_ecr_repository.api.arn]
  }

  # Só o documento de deploy, e só nesta instância. O GitHub não consegue mandar
  # comando arbitrário para a máquina.
  statement {
    sid     = "DispararDeploy"
    actions = ["ssm:SendCommand"]
    resources = [
      aws_ssm_document.deploy.arn,
      aws_instance.api.arn,
    ]
  }

  # Acompanhar o resultado do comando. A AWS não permite restringir por recurso aqui.
  statement {
    sid       = "AcompanharDeploy"
    actions   = ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "deploy" {
  name   = "readrace-deploy"
  role   = aws_iam_role.deploy.id
  policy = data.aws_iam_policy_document.deploy.json
}

# Comando fixo que a EC2 executa no deploy. O único parâmetro é o hash do commit,
# validado pelo padrão; com ele, os arquivos de infra/producao vêm daquele commit
# exato (o repositório é público) e a imagem com a mesma tag sobe.
resource "aws_ssm_document" "deploy" {
  name            = "readrace-deploy"
  document_type   = "Command"
  document_format = "YAML"

  content = <<-DOC
    schemaVersion: "2.2"
    description: "Atualiza a API do ReadRace para um commit da main"
    parameters:
      Commit:
        type: String
        description: "Hash completo do commit (40 caracteres)"
        allowedPattern: "^[0-9a-f]{40}$"
    mainSteps:
      - action: aws:runShellScript
        name: deploy
        inputs:
          timeoutSeconds: "900"
          runCommand:
            - set -euo pipefail
            - COMMIT="{{ Commit }}"
            - mkdir -p /opt/readrace && cd /opt/readrace
            - BASE="https://raw.githubusercontent.com/${local.repositorio_github}/$COMMIT/infra/producao"
            - ARQUIVOS="docker-compose.yml Caddyfile subir.sh backup-banco.sh readrace-backup.service readrace-backup.timer saude.sh readrace-saude.service readrace-saude.timer"
            - for f in $ARQUIVOS; do curl -fsSL "$BASE/$f" -o "$f.novo"; done
            - for f in $ARQUIVOS; do mv "$f.novo" "$f"; done
            - chmod 755 subir.sh backup-banco.sh saude.sh
            - ./subir.sh "$${COMMIT:0:12}"
  DOC
}

output "role_deploy_arn" {
  description = "Vai na variável AWS_DEPLOY_ROLE_ARN do repositório no GitHub."
  value       = aws_iam_role.deploy.arn
}
