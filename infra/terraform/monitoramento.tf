# Alarmes por e-mail. Sem isso, API fora do ar, disco cheio ou backup parado só seriam
# descobertos quando alguém abrisse o app.
#
# A saúde da API, o disco e a idade do último backup são medidos na própria EC2 pelo
# infra/producao/saude.sh, a cada 5 minutos, e enviados como métricas do namespace
# ReadRace. Se a máquina parar, as métricas param de chegar e o alarme da API dispara
# do mesmo jeito.

locals {
  namespace_metricas = "ReadRace"
}

resource "aws_sns_topic" "alertas" {
  name = "readrace-alertas"
}

# A AWS manda um e-mail pedindo confirmação; até alguém clicar no link, nenhum alerta chega.
resource "aws_sns_topic_subscription" "alertas_email" {
  topic_arn = aws_sns_topic.alertas.arn
  protocol  = "email"
  endpoint  = var.email_alertas
}

resource "aws_sns_topic_subscription" "alertas_email_extras" {
  for_each  = toset(var.emails_alertas_extras)
  topic_arn = aws_sns_topic.alertas.arn
  protocol  = "email"
  endpoint  = each.value
}

# Dois períodos seguidos sem resposta (10 minutos): um deploy, que derruba a API por
# menos de um minuto, não chega a disparar.
resource "aws_cloudwatch_metric_alarm" "api_fora_do_ar" {
  alarm_name          = "readrace-api-fora-do-ar"
  alarm_description   = "A API não respondeu UP no /actuator/health por 10 minutos, ou a EC2 parou de enviar métricas."
  namespace           = local.namespace_metricas
  metric_name         = "ApiSaudavel"
  statistic           = "Minimum"
  period              = 300
  evaluation_periods  = 2
  comparison_operator = "LessThanThreshold"
  threshold           = 1
  # Sem métrica = máquina parada ou timer quebrado. Conta como falha.
  treat_missing_data = "breaching"
  alarm_actions      = [aws_sns_topic.alertas.arn]
  ok_actions         = [aws_sns_topic.alertas.arn]
}

resource "aws_cloudwatch_metric_alarm" "disco_cheio" {
  alarm_name          = "readrace-disco-cheio"
  alarm_description   = "Disco da EC2 acima de 80%. Postgres e Docker param quando o disco enche."
  namespace           = local.namespace_metricas
  metric_name         = "DiscoUsadoPercentual"
  statistic           = "Maximum"
  period              = 300
  evaluation_periods  = 1
  comparison_operator = "GreaterThanThreshold"
  threshold           = 80
  treat_missing_data  = "notBreaching" # máquina parada já dispara o alarme da API
  alarm_actions       = [aws_sns_topic.alertas.arn]
  ok_actions          = [aws_sns_topic.alertas.arn]
}

# O backup diário roda às 06:00 UTC. 26 horas dão 2 de folga antes de avisar.
resource "aws_cloudwatch_metric_alarm" "backup_atrasado" {
  alarm_name          = "readrace-backup-atrasado"
  alarm_description   = "Nenhum backup do banco concluído nas últimas 26 horas. Ver journalctl -u readrace-backup na EC2."
  namespace           = local.namespace_metricas
  metric_name         = "HorasDesdeUltimoBackup"
  statistic           = "Maximum"
  period              = 300
  evaluation_periods  = 1
  comparison_operator = "GreaterThanThreshold"
  threshold           = 26
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alertas.arn]
  ok_actions          = [aws_sns_topic.alertas.arn]
}

# Falha no hardware da AWS que hospeda a máquina (métrica gratuita). Além de avisar, manda
# a AWS recuperar a instância: ela volta em outro servidor físico com o mesmo IP, disco e
# configuração. Em 6 out 2026 o servidor falhou, a recuperação automática padrão da AWS não
# aconteceu e a API ficou 44 horas fora até alguém parar e ligar a máquina à mão.
# A ação "recover" só é aceita com StatusCheckFailed_System; falha dentro do sistema
# operacional derruba a API e cai no alarme api_fora_do_ar.
resource "aws_cloudwatch_metric_alarm" "ec2_com_defeito" {
  alarm_name          = "readrace-ec2-com-defeito"
  alarm_description   = "Falha no hardware da AWS que hospeda a EC2. A AWS tenta recuperar a instância em outro servidor; se a API não voltar, parar e ligar a instância."
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed_System"
  dimensions          = { InstanceId = aws_instance.api.id }
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions = [
    "arn:aws:automate:${var.regiao}:ec2:recover",
    aws_sns_topic.alertas.arn,
  ]
  ok_actions = [aws_sns_topic.alertas.arn]
}

# Logs dos containers. O Docker de cada serviço envia a saída do container direto para o
# grupo dele (driver awslogs no infra/producao/docker-compose.yml), sem agente na máquina.
# Antes os logs só existiam no disco da EC2: com a falha de hardware de 6 out, teriam se
# perdido junto. Os grupos precisam existir antes do deploy: o Docker não sobe um container
# cujo grupo de logs não existe.
locals {
  servicos_com_log = toset(["api", "caddy", "db"])
}

resource "aws_cloudwatch_log_group" "servicos" {
  for_each = local.servicos_com_log
  name     = "/readrace/prod/${each.key}"
  # Sem retenção o CloudWatch guarda para sempre e o custo só cresce. 14 dias cobrem uma
  # sprint para investigar um problema.
  retention_in_days = 14
}

# Conta as linhas ERROR do log da API. O formato é o padrão do Spring Boot
# ("<data> ERROR <pid> --- ..."): o padrão olha só o segundo campo, então linhas de stack
# trace e mensagens que citam "ERROR" no texto não contam.
resource "aws_cloudwatch_log_metric_filter" "erros_api" {
  name           = "readrace-erros-api"
  log_group_name = aws_cloudwatch_log_group.servicos["api"].name
  pattern        = "[data, nivel = \"ERROR\", ...]"

  metric_transformation {
    name          = "ErrosApi"
    namespace     = local.namespace_metricas
    value         = "1"
    default_value = "0"
  }
}

# Um erro isolado (ex.: um 500 de uma chamada malformada) não manda e-mail; uma sequência,
# sim. Mais de 5 em 5 minutos indica algo quebrado para todo mundo.
resource "aws_cloudwatch_metric_alarm" "erros_api" {
  alarm_name          = "readrace-erros-na-api"
  alarm_description   = "Mais de 5 linhas ERROR no log da API em 5 minutos. Ver o grupo /readrace/prod/api no CloudWatch Logs."
  namespace           = local.namespace_metricas
  metric_name         = aws_cloudwatch_log_metric_filter.erros_api.metric_transformation[0].name
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  comparison_operator = "GreaterThanThreshold"
  threshold           = 5
  treat_missing_data  = "notBreaching" # máquina parada já dispara o alarme da API
  alarm_actions       = [aws_sns_topic.alertas.arn]
  ok_actions          = [aws_sns_topic.alertas.arn]
}
