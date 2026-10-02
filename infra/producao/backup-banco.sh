#!/bin/bash
# Copia o banco de produção para o S3. Uso: ./backup-banco.sh [diario|antes-do-deploy]
#
# Roda todo dia pelo timer do systemd (readrace-backup.timer) e antes de cada deploy
# pelo subir.sh. Os arquivos expiram sozinhos no bucket depois de 30 dias.
# Restaurar: ver "Backup do banco" em infra/README.md.
set -euo pipefail

MOTIVO="${1:-diario}"
REGIAO=us-east-2
CONTA=$(aws sts get-caller-identity --region "$REGIAO" --query Account --output text)
BUCKET="readrace-backup-$CONTA"
CHAVE="$MOTIVO/readrace-$(date -u +%Y-%m-%dT%H%M%SZ).dump"

DB=$(docker ps -q --filter label=com.docker.compose.project=readrace --filter label=com.docker.compose.service=db)
if [ -z "$DB" ]; then
  echo "Container do banco não está rodando; nada para copiar." >&2
  exit 1
fi

# Arquivo temporário em vez de mandar direto pelo pipe: se o pg_dump falhar no meio,
# nada é enviado, em vez de um dump cortado que parece válido.
ARQUIVO=$(mktemp /var/tmp/readrace-backup.XXXXXX)
trap 'rm -f "$ARQUIVO"' EXIT

# Formato custom (-Fc): já sai comprimido e é restaurado com pg_restore.
docker exec "$DB" pg_dump -U readrace -d readrace -Fc > "$ARQUIVO"

aws s3 cp "$ARQUIVO" "s3://$BUCKET/$CHAVE" --region "$REGIAO" --only-show-errors
echo "Backup enviado: s3://$BUCKET/$CHAVE ($(du -h "$ARQUIVO" | cut -f1))"
