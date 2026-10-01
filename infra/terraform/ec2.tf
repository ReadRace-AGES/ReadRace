# Instância criada pelo console e importada. Os valores de ami, subnet, IP privado,
# key pair e criptografia do disco reproduzem o que existe: mudar qualquer um deles faz
# o Terraform destruir e recriar a máquina, junto com o Postgres que roda nela.
# Tipo da instância e tamanho do disco mudam sem recriar.
resource "aws_instance" "api" {
  ami = "ami-0d3d85815a9746bc5"
  # t3.small (2 GB) cabe no teto de US$ 28,27/mês; a t3.medium sob demanda sozinha passa dele.
  # Medido: a API sobrevive a carga com 512 MB e é morta com 384 MB. Trocar o tipo para e
  # religa a instância (sem recriar), e o IP público muda enquanto não houver Elastic IP.
  instance_type               = "t3.small"
  availability_zone           = "us-east-2a"
  subnet_id                   = "subnet-0c2e3ab760be82509"
  private_ip                  = "172.31.6.27"
  associate_public_ip_address = true
  key_name                    = "Ec2ReadRace"
  vpc_security_group_ids      = [aws_security_group.api.id]
  iam_instance_profile        = aws_iam_instance_profile.api.name

  # Proteção contra encerramento, já ligada no console. Fica explícita para ninguém desligar sem ver.
  disable_api_termination = true

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required" # IMDSv2 obrigatório
    http_put_response_hop_limit = 2          # 2 para containers Docker alcançarem o IMDS
  }

  # Volume criado sem criptografia. Ligar agora exige um volume novo; fica como limitação conhecida.
  root_block_device {
    volume_type = "gp3"
    volume_size = 20 # disco só cresce: a AWS não permite reduzir um volume EBS
    encrypted   = false
  }

  tags = {
    Name = "ReadRace"
  }

  lifecycle {
    prevent_destroy = true
  }
}
