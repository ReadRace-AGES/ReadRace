output "ip_publico" {
  description = "Elastic IP da API."
  value       = aws_eip.api.public_ip
}

# Sem domínio comprado, o sslip.io resolve o próprio IP escrito no nome
# (52-1-2-3.sslip.io -> 52.1.2.3), o que basta para emitir certificado Let's Encrypt.
output "host_api" {
  description = "Nome público da API, sem custo de domínio."
  value       = "${replace(aws_eip.api.public_ip, ".", "-")}.sslip.io"
}
