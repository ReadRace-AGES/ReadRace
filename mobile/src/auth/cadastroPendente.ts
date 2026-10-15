/**
 * E-mail e senha de quem criou a conta (ou tentou entrar sem confirmar) e ainda não digitou o
 * código. Só em memória: a senha nunca vira parâmetro de navegação nem vai para o disco. Serve para
 * o passo 2 entrar direto depois de confirmar. Sem dependências do app, testado com node:test.
 */

export const ESPERA_REENVIO_MS = 60_000;

export type CadastroPendente = {
  email: string;
  senha: string;
  ultimoEnvio: number | null;
};

let pendente: CadastroPendente | null = null;

/** `ultimoEnvio` é null quando nenhum código foi enviado agora (ex.: veio do login). */
export function guardarCadastro(
  email: string,
  senha: string,
  ultimoEnvio: number | null
): void {
  // Sem envio novo e mesmo cadastro (ex.: login logo depois de criar a conta): a espera do
  // "Reenviar código" continua valendo.
  const envio =
    ultimoEnvio === null && ehOMesmoCadastro(email, senha)
      ? (pendente?.ultimoEnvio ?? null)
      : ultimoEnvio;
  pendente = { email: email.trim(), senha, ultimoEnvio: envio };
}

export function cadastroPendente(): CadastroPendente | null {
  return pendente;
}

export function registrarReenvio(agora: number): void {
  if (pendente) {
    pendente = { ...pendente, ultimoEnvio: agora };
  }
}

export function limparCadastro(): void {
  pendente = null;
}

/** O pool não diferencia maiúsculas no e-mail (case_sensitive = false); a senha, sim. */
export function ehOMesmoCadastro(email: string, senha: string): boolean {
  return (
    pendente !== null &&
    pendente.email.toLowerCase() === email.trim().toLowerCase() &&
    pendente.senha === senha
  );
}

/** Quantos segundos faltam para liberar o "Reenviar código". 0 = liberado. */
export function segundosParaReenviar(
  ultimoEnvio: number | null,
  agora: number
): number {
  if (ultimoEnvio === null) {
    return 0;
  }
  return Math.max(
    0,
    Math.ceil((ultimoEnvio + ESPERA_REENVIO_MS - agora) / 1000)
  );
}
