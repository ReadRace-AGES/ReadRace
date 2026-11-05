import { apiGet, apiRequest } from '@/api/client';

/**
 * A Página do clube só existe para `ClubeDoLivro`: comunidade não tem livro atual nem ranking.
 * A moeda do ranking é `Pontos`, do clube — `XP` nunca aparece aqui.
 */
export type Clube = {
  id: string;
  nome: string;
  livroAtual: { id: string; titulo: string; autor: string | null };
  ranking: LinhaRanking[];
  /** Cargo do usuário atual neste clube; `null` quando ele não participa (#160). */
  meuCargo: 'administrador' | 'membro' | null;
  /** Se o clube já tem quiz publicado: o líder não pode abrir `GET /quiz` para descobrir (#161). */
  temQuizAtivo: boolean;
  proximoLider: { id: string; nome: string; avatarUrl: string | null } | null;
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

export type SaidaDoClube = {
  novoLider: { id: string; nome: string } | null;
};

export function sairDoClube(clubeId: string): Promise<SaidaDoClube> {
  return apiRequest<SaidaDoClube>(
    `/api/clubes/${encodeURIComponent(clubeId)}/membros/eu`,
    { method: 'DELETE' }
  );
}

export type RankingClube = {
  clube: { id: string; nome: string };
  ranking: LinhaRanking[];
  proximoCursor: string | null;
};

export function buscarRankingClube(
  clubeId: string,
  cursor: string | null,
  signal?: AbortSignal
) {
  const consulta = cursor ? `?cursor=${encodeURIComponent(cursor)}` : '';
  return apiGet<RankingClube>(
    `/api/clubes/${encodeURIComponent(clubeId)}/ranking${consulta}`,
    { signal }
  );
}