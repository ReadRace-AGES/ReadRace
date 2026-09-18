import { apiGet } from '@/api/client';

/**
 * O Fórum é do clube: um `Post` pertence a um `ClubeDoLivro` ou a uma `Comunidade`, nunca aos
 * dois, e comentário é post com pai e não entra na lista.
 */
export type ForumClube = {
  clube: {
    id: string;
    nome: string;
    livroAtual: {
      titulo: string;
      autor: string | null;
      capaUrl: string | null;
    };
  };
  posts: PostForum[];
};

export type PostForum = {
  id: string;
  autor: {
    id: string;
    nome: string | null;
    avatarUrl: string | null;
    sequenciaDias: number | null;
  };
  publicadoEm: string;
  texto: string;
  /** Só exibição: não há ação de curtir nesta sprint. */
  totalCurtidas: number;
};

/** `GET /api/clubes/{clubeId}/posts` — já ordenado do mais recente para o mais antigo. */
export function buscarForumDoClube(clubeId: string, signal?: AbortSignal) {
  return apiGet<ForumClube>(
    `/api/clubes/${encodeURIComponent(clubeId)}/posts`,
    { signal }
  );
}
