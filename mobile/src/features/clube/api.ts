import { apiGet, apiRequest } from '@/api/client';

/**
 * Dados da página de um clube do livro.
 * O ranking utiliza pontos do clube, não XP.
 */
export type Clube = {
  id: string;
  nome: string;
  livroAtual: {
    id: string;
    titulo: string;
    autor: string | null;
  };
  ranking: LinhaRanking[];
  meuCargo: 'administrador' | 'membro' | null;
};

export type LinhaRanking = {
  posicao: number;
  usuario: {
    id: string;
    nome: string;
    avatarUrl: string | null;
  };
  pontos: number;
};

/** Busca os detalhes do clube e seu ranking. */
export function buscarClube(clubeId: string, signal?: AbortSignal) {
  return apiGet<Clube>(`/api/clubes/${encodeURIComponent(clubeId)}`, {
    signal,
  });
}

/**
 * Remove o usuário autenticado do clube.
 *
 * 204: saída realizada com sucesso.
 * 404: usuário não é membro.
 * 409: usuário é o único administrador e há outros membros.
 */
export async function sairDoClube(clubeId: string): Promise<void> {
  await apiRequest<void>(
    `/api/clubes/${encodeURIComponent(clubeId)}/membros/eu`,
    { method: 'DELETE' }
  );
}
