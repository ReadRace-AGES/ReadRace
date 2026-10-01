provider "aws" {
  region = var.regiao

  # Toda tag aqui vai para todos os recursos criados por este código, o que separa
  # no console o que é gerenciado pelo Terraform do que foi feito à mão.
  default_tags {
    tags = {
      Projeto       = "readrace"
      GerenciadoPor = "terraform"
    }
  }
}
