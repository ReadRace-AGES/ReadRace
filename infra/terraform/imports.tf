# Recursos criados pelo console antes do Terraform. O bloco import só traz o recurso
# para o state; quem descreve a configuração é o código em ec2.tf, rede.tf e cognito.tf.
# Depois do primeiro apply estes blocos podem ser apagados, mas manter não faz mal:
# import de algo que já está no state é ignorado.

import {
  to = aws_instance.api
  id = "i-0a27ef464e5619692"
}

import {
  to = aws_security_group.api
  id = "sg-07583c06946ce374a"
}

import {
  to = aws_cognito_user_pool.principal
  id = "us-east-2_MEVIpejhy"
}

import {
  to = aws_cognito_user_pool_client.app
  id = "us-east-2_MEVIpejhy/5f06kagodbku0mvveakmsim8qu"
}

import {
  to = aws_cognito_user_pool_domain.principal
  id = "us-east-2mevipejhy"
}

import {
  to = aws_cognito_managed_login_branding.app
  id = "us-east-2_MEVIpejhy,73d54128-03ce-45e5-b0d8-d4da7193a6d4"
}
