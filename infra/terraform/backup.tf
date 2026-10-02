# Backup diário do Postgres. Sem RDS não há snapshot automático: o banco vive num
# volume Docker no disco da EC2, e uma cópia nesse mesmo disco morreria junto com ele.
# O pg_dump roda na EC2 (infra/producao/backup-banco.sh) e o arquivo vem para cá.
resource "aws_s3_bucket" "backup" {
  bucket = "readrace-backup-${data.aws_caller_identity.atual.account_id}"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "backup" {
  bucket                  = aws_s3_bucket.backup.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backup" {
  bucket = aws_s3_bucket.backup.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 30 dias cobrem uma sprint inteira com folga para alguém perceber um dado errado.
# A expiração é feita pela própria AWS: nenhum script precisa apagar nada, e por isso
# a EC2 não tem permissão de apagar.
resource "aws_s3_bucket_lifecycle_configuration" "backup" {
  bucket = aws_s3_bucket.backup.id

  rule {
    id     = "expira-em-30-dias"
    status = "Enabled"
    filter {}
    expiration {
      days = 30
    }
    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

data "aws_iam_policy_document" "backup_bucket" {
  statement {
    sid     = "SoTls"
    effect  = "Deny"
    actions = ["s3:*"]
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    resources = [aws_s3_bucket.backup.arn, "${aws_s3_bucket.backup.arn}/*"]
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "backup" {
  bucket = aws_s3_bucket.backup.id
  policy = data.aws_iam_policy_document.backup_bucket.json

  depends_on = [aws_s3_bucket_public_access_block.backup]
}

# A EC2 grava e lê (a restauração roda nela), mas não apaga. Se a máquina for
# comprometida, quem a controla já tem o banco; o que não pode é destruir as cópias.
data "aws_iam_policy_document" "api_backup" {
  statement {
    sid       = "GravarELerBackups"
    actions   = ["s3:PutObject", "s3:GetObject"]
    resources = ["${aws_s3_bucket.backup.arn}/*"]
  }

  statement {
    sid       = "ListarBackups"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.backup.arn]
  }
}

resource "aws_iam_role_policy" "api_backup" {
  name   = "readrace-api-backup"
  role   = aws_iam_role.api.id
  policy = data.aws_iam_policy_document.api_backup.json
}

output "bucket_backup" {
  description = "Bucket dos backups do Postgres."
  value       = aws_s3_bucket.backup.bucket
}
