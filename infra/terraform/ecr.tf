# Repositório das imagens da API. O GitHub Actions envia, a EC2 baixa.
resource "aws_ecr_repository" "api" {
  name = "readrace-api"

  # Tag imutável: cada imagem leva o hash do commit e nunca é sobrescrita, então
  # sempre dá para saber o que está no ar e voltar para a versão anterior.
  image_tag_mutability = "IMMUTABLE"

  # Varredura básica de vulnerabilidades a cada envio. Não tem custo.
  image_scanning_configuration {
    scan_on_push = true
  }
}

# Guarda só as 10 imagens mais recentes. Mantém o armazenamento dentro dos 5 GB
# previstos no orçamento e ainda deixa margem para voltar algumas versões.
resource "aws_ecr_lifecycle_policy" "api" {
  repository = aws_ecr_repository.api.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Mantem as 10 imagens mais recentes"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
