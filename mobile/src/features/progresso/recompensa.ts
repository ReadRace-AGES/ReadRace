import type { ProgressoLeituraResponse } from './api';

type Recompensa = Pick<
  ProgressoLeituraResponse,
  | 'xpPaginas'
  | 'xpConclusao'
  | 'sequenciaDias'
  | 'nivel'
  | 'subiuDeNivel'
  | 'conquistasDesbloqueadas'
>;

/**
 * Uma linha do aviso por item ganho, na ordem XP, livro concluído, nível e conquistas. Sem nada
 * ganho a lista vem vazia e nenhum aviso aparece.
 */
export function linhasDaRecompensa(recompensa: Recompensa): string[] {
  const linhas: string[] = [];

  // Página repetida ou menor não rende XP: a linha de XP (e a sequência junto) fica de fora.
  if (recompensa.xpPaginas > 0) {
    const dias = recompensa.sequenciaDias;
    linhas.push(
      `+${recompensa.xpPaginas} XP · ${dias} ${dias === 1 ? 'dia' : 'dias'} de sequência`
    );
  }
  if (recompensa.xpConclusao > 0) {
    linhas.push(`Livro concluído! +${recompensa.xpConclusao} XP`);
  }
  if (recompensa.subiuDeNivel) {
    linhas.push(`Você subiu para o nível ${recompensa.nivel}!`);
  }
  for (const conquista of recompensa.conquistasDesbloqueadas ?? []) {
    linhas.push(`Conquista desbloqueada: ${conquista.nome}`);
  }

  return linhas;
}
