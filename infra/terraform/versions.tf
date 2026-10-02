terraform {
  # 1.11 é a primeira versão com lock de state direto no S3 (use_lockfile), sem DynamoDB.
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.67"
    }
  }

  # Configuração parcial: bucket, chave e região vêm de backend.hcl (fora do git),
  # porque o bloco backend não aceita variáveis.
  # Uso: terraform init -backend-config=backend.hcl
  backend "s3" {}
}
