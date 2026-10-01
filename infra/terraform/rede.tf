# Security group criado pelo assistente do console e importado. Nome e descrição
# não mudam: a AWS não permite editá-los, e o Terraform recriaria o grupo.
resource "aws_security_group" "api" {
  name        = "launch-wizard-1"
  description = "launch-wizard-1 created 2026-10-01T01:39:36.299Z"
  vpc_id      = "vpc-0c1e127530bd8ff31"

  # Só HTTPS entra. Sem porta 22: o acesso à máquina é pelo SSM Session Manager.
  ingress {
    description = ""
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = ""
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    prevent_destroy = true
  }
}

# IP fixo da API. Sem ele o IP público muda a cada parada da instância, e o endereço
# sslip.io e o certificado HTTPS dependem dele. Associado, substitui o IP público
# automático (que é liberado), então continua sendo um único IPv4 cobrado.
# Atenção: Elastic IP solto, sem instância, é cobrado do mesmo jeito.
resource "aws_eip" "api" {
  domain   = "vpc"
  instance = aws_instance.api.id

  tags = {
    Name = "ReadRace"
  }

  # Liberar o IP perde o endereço para sempre: o nome sslip.io e o certificado mudam.
  lifecycle {
    prevent_destroy = true
  }
}
