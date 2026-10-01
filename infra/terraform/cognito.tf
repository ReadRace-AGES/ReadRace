# Cognito criado pelo console e importado. Login por e-mail; o app fala direto com o
# Cognito e a API só valida o token (ADR de autenticação, issue #131).

resource "aws_cognito_user_pool" "principal" {
  # O nome não pode mudar: o Cognito recriaria o pool, apagando todas as contas.
  name           = "User pool - raowmw"
  user_pool_tier = "ESSENTIALS"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]
  mfa_configuration        = "OFF"
  deletion_protection      = "ACTIVE"

  username_configuration {
    case_sensitive = false
  }

  sign_in_policy {
    allowed_first_auth_factors = ["PASSWORD"]
  }

  # Política em aberto: decisão do time. Mudar aqui é in-place.
  password_policy {
    minimum_length                   = 8
    require_lowercase                = true
    require_uppercase                = true
    require_numbers                  = true
    require_symbols                  = true
    password_history_size            = 0
    temporary_password_validity_days = 7
  }

  # Cadastro pelo próprio usuário, pela tela "Criar conta" do app.
  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
    recovery_mechanism {
      name     = "verified_phone_number"
      priority = 2
    }
  }

  # Envio gratuito do próprio Cognito, limitado a cerca de 50 e-mails por dia.
  email_configuration {
    email_sending_account = "COGNITO_DEFAULT"
  }

  # O Cognito exige o marcador {####}, que vira o código de confirmação.
  verification_message_template {
    default_email_option = "CONFIRM_WITH_CODE"
    email_subject        = "Seu código do ReadRace"
    email_message        = "Seu código de confirmação do ReadRace é {####}. Ele vale por 24 horas."
  }

  # Atributos obrigatórios desde a criação. O Cognito não permite alterá-los.
  schema {
    name                     = "email"
    attribute_data_type      = "String"
    required                 = true
    mutable                  = true
    developer_only_attribute = false
    string_attribute_constraints {
      min_length = "0"
      max_length = "2048"
    }
  }

  schema {
    name                     = "name"
    attribute_data_type      = "String"
    required                 = true
    mutable                  = true
    developer_only_attribute = false
    string_attribute_constraints {
      min_length = "0"
      max_length = "2048"
    }
  }

  lifecycle {
    prevent_destroy = true
    # Se o provider enxergar diferença no schema, o plan propõe recriar o pool. Recriar
    # apaga todas as contas, então mudança de schema nunca passa pelo Terraform.
    ignore_changes = [schema]
  }
}

resource "aws_cognito_user_pool_client" "app" {
  name         = "ReadRace - cognito"
  user_pool_id = aws_cognito_user_pool.principal.id

  # App mobile não guarda segredo com segurança, então o client não tem secret. Não declarar
  # generate_secret aqui: o client foi criado sem o atributo, e declarar false força recriar
  # o client, trocando o ID que o app usa.

  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_AUTH",
    "ALLOW_USER_PASSWORD_AUTH", # login do app por e-mail e senha, sem Amplify
    "ALLOW_USER_SRP_AUTH",
  ]
  supported_identity_providers = ["COGNITO"]

  # O escopo aws.cognito.signin.user.admin autoriza o GetUser que a API faz no primeiro
  # acesso. O fluxo OAuth (Google, etapa 2) só traz o escopo se ele estiver liberado aqui.
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["email", "openid", "aws.cognito.signin.user.admin"]
  callback_urls                        = ["readrace://auth"]
  logout_urls                          = ["readrace://logout"]

  # Acesso de 1 hora; renovação de 30 dias para quem usa o app todo dia não ser deslogado.
  access_token_validity  = 60
  id_token_validity      = 60
  refresh_token_validity = 30
  auth_session_validity  = 3
  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }

  # O "sair" invalida o refresh token de verdade, e o login não revela se o e-mail existe.
  enable_token_revocation       = true
  prevent_user_existence_errors = "ENABLED"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_cognito_user_pool_domain" "principal" {
  domain                = "us-east-2mevipejhy"
  user_pool_id          = aws_cognito_user_pool.principal.id
  managed_login_version = 2
}

output "cognito_user_pool_id" {
  description = "Vai em READRACE_COGNITO_USER_POOL_ID e na configuração do app."
  value       = aws_cognito_user_pool.principal.id
}

output "cognito_client_id" {
  description = "Vai em READRACE_COGNITO_CLIENT_ID e em EXPO_PUBLIC_COGNITO_CLIENT_ID."
  value       = aws_cognito_user_pool_client.app.id
}
