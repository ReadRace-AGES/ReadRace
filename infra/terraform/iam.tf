# Role assumida pela própria EC2. O SSM dá acesso ao terminal da máquina pelo Session
# Manager sem porta 22 aberta. O acesso ao bucket de backup fica em backup.tf.
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

# O que a EC2 precisa para rodar a aplicação, e só isso: baixar a imagem da API, ler
# os segredos de produção e enviar as métricas de saúde e os logs dos containers.
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

  # Métricas do saude.sh, só no namespace do projeto. PutMetricData não aceita
  # restrição por recurso; a condição é o que limita.
  statement {
    sid       = "EnviarMetricas"
    actions   = ["cloudwatch:PutMetricData"]
    resources = ["*"]
    condition {
      test     = "StringEquals"
      variable = "cloudwatch:namespace"
      values   = ["ReadRace"]
    }
  }

  # Logs dos containers, só nos grupos do projeto. Os grupos são do Terraform
  # (monitoramento.tf), então o Docker não precisa de logs:CreateLogGroup.
  statement {
    sid       = "EnviarLogs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [for grupo in aws_cloudwatch_log_group.servicos : "${grupo.arn}:*"]
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
