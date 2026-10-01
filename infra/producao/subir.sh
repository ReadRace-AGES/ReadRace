#!/bin/bash
# Sobe ou atualiza a produção na EC2. Uso: ./subir.sh <tag-da-imagem>
#
# Os segredos são lidos do Parameter Store a cada execução e só existem nas variáveis
# de ambiente deste processo e dos containers: nada é gravado em arquivo na máquina.
set -euo pipefail

TAG="${1:?informe a tag da imagem (hash do commit)}"
REGIAO=us-east-2
CONTA=$(aws sts get-caller-identity --region "$REGIAO" --query Account --output text)
REGISTRO="$CONTA.dkr.ecr.$REGIAO.amazonaws.com"
IP=$(curl -fs -H "X-aws-ec2-metadata-token: $(curl -fs -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')" \
  http://169.254.169.254/latest/meta-data/public-ipv4)

parametro() {
  aws ssm get-parameter --region "$REGIAO" --name "$1" --with-decryption --query Parameter.Value --output text
}

export IMAGEM_API="$REGISTRO/readrace-api:$TAG"
export HOST_API="${IP//./-}.sslip.io"
export DB_PASSWORD="$(parametro /readrace/prod/db/senha)"
export GOOGLE_BOOKS_API_KEY="$(parametro /readrace/prod/google-books/api-key)"

aws ecr get-login-password --region "$REGIAO" | docker login --username AWS --password-stdin "$REGISTRO" >/dev/null

cd "$(dirname "$0")"
docker compose pull --quiet
docker compose up -d --remove-orphans
docker image prune -f >/dev/null

echo "Imagem no ar: $IMAGEM_API"
echo "Endereço: https://$HOST_API"
