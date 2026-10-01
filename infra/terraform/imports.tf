# Recursos criados pelo console antes do Terraform. O bloco import só traz o recurso
# para o state; quem descreve a configuração é o código em ec2.tf e rede.tf.
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
