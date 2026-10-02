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

# Métrica que a AWS mede de graça: falha no hardware que hospeda a máquina ou no
# sistema operacional dela.
resource "aws_cloudwatch_metric_alarm" "ec2_com_defeito" {
  alarm_name          = "readrace-ec2-com-defeito"
  alarm_description   = "A EC2 falhou na verificação de status da AWS (hardware ou sistema operacional)."
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed"
  dimensions          = { InstanceId = aws_instance.api.id }
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alertas.arn]
  ok_actions          = [aws_sns_topic.alertas.arn]
}
