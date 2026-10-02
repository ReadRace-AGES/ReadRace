import { apiGet } from '@/api/client';

/**
 * A Página do clube só existe para `ClubeDoLivro`: comunidade não tem livro atual nem ranking.
 * A moeda do ranking é `Pontos`, do clube — `XP` nunca aparece aqui.
 */
export type Clube = {
  id: string;
  nome: string;
  livroAtual: { id: string; titulo: string; autor: string | null };
  ranking: LinhaRanking[];
};

export type LinhaRanking = {
  posicao: number;
  usuario: { id: string; nome: string; avatarUrl: string | null };
  pontos: number;
};

/** `GET /api/clubes/{clubeId}` — o ranking já chega ordenado e numerado pelo backend. */
export function buscarClube(clubeId: string, signal?: AbortSignal) {
  return apiGet<Clube>(`/api/clubes/${encodeURIComponent(clubeId)}`, {
    signal,
  });
}
