variable "regiao" {
  description = "Região da AWS onde a EC2 e o Cognito já existem."
  type        = string
  default     = "us-east-2"
}

# Fica no terraform.tfvars (fora do git): o repositório é público.
variable "email_alertas" {
  description = "E-mail que recebe os alarmes do CloudWatch. Precisa confirmar a inscrição pelo link que a AWS envia."
  type        = string
}
