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

# Cliente OAuth "Aplicativo da Web" do projeto readrace-prod no Google Cloud, usado pelo Cognito
# no login com Google (#158). Os dois ficam no terraform.tfvars: o repositório é público.
variable "google_client_id" {
  description = "ID do cliente OAuth do Google (termina em .apps.googleusercontent.com)."
  type        = string
}

variable "google_client_secret" {
  description = "Chave secreta do cliente OAuth do Google. Vai para o state, criptografado no S3."
  type        = string
  sensitive   = true
}
