import type { ResumoQuiz } from './api';

/**
 * Recado do quiz para a Página do clube, que fica embaixo na pilha.
 *
 * O toast não serve para isso: o `ToastProvider` esconde o aviso quando a rota muda, e voltar
 * do quiz muda a rota. Então o quiz deixa o recado aqui e a Página do clube, ao ganhar foco,
 * recarrega o ranking e só depois mostra o resumo.
 */
type Retorno = { resumo: ResumoQuiz | null };

const pendentes = new Map<string, Retorno>();

/** Cada resposta mexe nos `Pontos` do clube: a página precisa recarregar o ranking. */
export function marcarRespostaNoClube(clubeId: string) {
  if (!pendentes.has(clubeId)) pendentes.set(clubeId, { resumo: null });
}

export function marcarQuizConcluido(clubeId: string, resumo: ResumoQuiz) {
  pendentes.set(clubeId, { resumo });
}

export function consumirRetorno(clubeId: string): Retorno | undefined {
  const retorno = pendentes.get(clubeId);
  pendentes.delete(clubeId);
  return retorno;
}

export function textoDoResumo({ acertos, total, pontos, xp }: ResumoQuiz) {
  return `Quiz concluído: ${acertos} de ${total} acertos, +${pontos} pontos e +${xp} XP.`;
}
