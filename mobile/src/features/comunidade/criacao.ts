export const LIMITES_COMUNIDADE = {
  nome: 120,
  descricao: 2000,
  imagemUrl: 2048,
};

export function erroNomeComunidade(nome: string): string | null {
  const valor = nome.trim();
  if (!valor) return 'Dê um nome à sua comunidade.';
  if (valor.length > LIMITES_COMUNIDADE.nome || /\p{Cc}/u.test(valor))
    return 'Use até 120 caracteres, sem quebras de linha.';
  return null;
}

export function erroImagemComunidade(imagemUrl: string): string | null {
  const valor = imagemUrl.trim();
  if (!valor) return null;
  try {
    const url = new URL(valor);
    if (
      valor.length <= LIMITES_COMUNIDADE.imagemUrl &&
      /^https:\/\/[^/]/i.test(valor) &&
      !/[\s\\]/u.test(valor) &&
      url.hostname &&
      !url.username &&
      !url.password
    )
      return null;
  } catch {
    // Mantém a mesma mensagem para qualquer URL inválida.
  }
  return 'Use um link HTTPS válido, sem usuário ou senha (até 2.048 caracteres).';
}
