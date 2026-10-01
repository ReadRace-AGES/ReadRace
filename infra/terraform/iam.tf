# Role assumida pela própria EC2. O SSM dá acesso ao terminal da máquina pelo Session
# Manager sem porta 22 aberta. Escrita no bucket de backup e envio de logs entram
# aqui conforme esses recursos forem criados.
data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "api" {
  name               = "readrace-api-ec2"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "api_ssm" {
  role       = aws_iam_role.api.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_caller_identity" "atual" {}

# O que a EC2 precisa para rodar a aplicação, e só isso: baixar a imagem da API e
# ler os segredos de produção.
data "aws_iam_policy_document" "api_runtime" {
  # O token de login do ECR não é por repositório; a AWS só aceita "*" aqui.
  statement {
    sid       = "LoginNoEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "BaixarImagemDaApi"
    actions = [
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchCheckLayerAvailability",
    ]
    resources = [aws_ecr_repository.api.arn]
  }

  # Os parâmetros são criados à mão no console, fora do Terraform: se o Terraform
  # os gerenciasse, o valor real da senha acabaria gravado no state.
  statement {
    sid     = "LerSegredosDeProducao"
    actions = ["ssm:GetParameter", "ssm:GetParameters", "ssm:GetParametersByPath"]
    resources = [
      "arn:aws:ssm:${var.regiao}:${data.aws_caller_identity.atual.account_id}:parameter/readrace/prod/*",
    ]
  }
}

resource "aws_iam_role_policy" "api_runtime" {
  name   = "readrace-api-runtime"
  role   = aws_iam_role.api.id
  policy = data.aws_iam_policy_document.api_runtime.json
}

# A EC2 não recebe a role direto, e sim um instance profile que a embrulha.
resource "aws_iam_instance_profile" "api" {
  name = "readrace-api-ec2"
  role = aws_iam_role.api.name
}
