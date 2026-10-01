# Role assumida pela própria EC2. Começa só com o SSM, que dá acesso ao terminal da
# máquina pelo Session Manager sem porta 22 aberta. Pull do ECR, escrita no bucket
# de backup e envio de logs entram aqui conforme esses recursos forem criados.
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

# A EC2 não recebe a role direto, e sim um instance profile que a embrulha.
resource "aws_iam_instance_profile" "api" {
  name = "readrace-api-ec2"
  role = aws_iam_role.api.name
}
