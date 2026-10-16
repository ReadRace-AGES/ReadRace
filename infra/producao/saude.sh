#!/bin/bash
# Mede a saúde da produção e envia ao CloudWatch. Roda a cada 5 minutos pelo
# readrace-saude.timer. Os alarmes estão em infra/terraform/monitoramento.tf.
#
#   ApiSaudavel             1 se o /actuator/health respondeu UP pelo Caddy, senão 0
#   DiscoUsadoPercentual    uso do disco raiz
#   HorasDesdeUltimoBackup  horas desde o último backup concluído (backup-banco.sh)
#
# Sem "set -e": uma medida que falhe não pode impedir o envio das outras.
set -uo pipefail

REGIAO=us-east-2
MARCA_BACKUP=/var/lib/readrace/ultimo-backup

TOKEN=$(curl -fs -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
IP=$(curl -fs -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4)
HOST="${IP//./-}.sslip.io"

# Pelo Caddy, com o certificado de verdade, mas sem sair da máquina (--resolve):
# testa o mesmo caminho que o app usa.
# Resposta numa variável em vez de "curl | grep -q": com pipefail, o grep que sai cedo
# pode matar o curl com SIGPIPE e a API seria dada como fora do ar sem estar.
RESPOSTA=$(curl -fsS -m 10 --resolve "$HOST:443:127.0.0.1" "https://$HOST/actuator/health")
if [[ "$RESPOSTA" == *'"status":"UP"'* ]]; then
  API=1
else
  API=0
fi

DISCO=$(df --output=pcent / | tail -1 | tr -dc '0-9')

if [ -f "$MARCA_BACKUP" ]; then
  IDADE=$(( ($(date +%s) - $(stat -c %Y "$MARCA_BACKUP")) / 3600 ))
else
  IDADE=9999
fi

aws cloudwatch put-metric-data --region "$REGIAO" --namespace ReadRace --metric-data \
  "MetricName=ApiSaudavel,Value=$API,Unit=Count" \
  "MetricName=DiscoUsadoPercentual,Value=$DISCO,Unit=Percent" \
  "MetricName=HorasDesdeUltimoBackup,Value=$IDADE,Unit=Count"

echo "api=$API disco=${DISCO}% backup=${IDADE}h"
