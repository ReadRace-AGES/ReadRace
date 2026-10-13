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

# Pessoas que também recebem os alarmes. Em 6 out 2026 os avisos chegaram só no e-mail do
# projeto, ninguém leu, e a API ficou 44 horas fora. Também fica no terraform.tfvars.
variable "emails_alertas_extras" {
  description = "E-mails pessoais que também recebem os alarmes. Cada um confirma o link que a AWS envia."
  type        = list(string)
  default     = []
}
