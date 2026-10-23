import type { Comentario } from './api';

/** Aplica `transformar` no comentário `id`, em qualquer nível da thread. */
export function comComentario(
  lista: Comentario[],
  id: string,
  transformar: (comentario: Comentario) => Comentario
): Comentario[] {
  return lista.map((comentario) =>
    comentario.id === id
      ? transformar(comentario)
      : comentario.respostas.length === 0
        ? comentario
        : {
            ...comentario,
            respostas: comComentario(comentario.respostas, id, transformar),
          }
  );
}

/** Põe `novo` no fim das respostas de `paiId`, ou no fim da thread sem pai. */
export function inserirNaThread(
  lista: Comentario[],
  paiId: string | null,
  novo: Comentario
): Comentario[] {
  if (paiId === null) return [...lista, novo];
  return comComentario(lista, paiId, (pai) => ({
    ...pai,
    respostas: [...pai.respostas, novo],
  }));
}

export function buscarComentario(
  lista: Comentario[],
  id: string
): Comentario | null {
  for (const comentario of lista) {
    if (comentario.id === id) return comentario;
    const achado = buscarComentario(comentario.respostas, id);
    if (achado) return achado;
  }
  return null;
}

/** Respostas abaixo do comentário, em todos os níveis. */
export function totalDeRespostas(comentario: Comentario): number {
  return comentario.respostas.reduce(
    (total, resposta) => total + 1 + totalDeRespostas(resposta),
    0
  );
}

/** ids dos comentários que têm respostas, em qualquer nível: o que "Recolher respostas" recolhe. */
export function idsComRespostas(lista: Comentario[]): string[] {
  return lista.flatMap((comentario) =>
    comentario.respostas.length === 0
      ? []
      : [comentario.id, ...idsComRespostas(comentario.respostas)]
  );
}
