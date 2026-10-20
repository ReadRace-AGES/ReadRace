import { apiGet, apiRequest } from '@/api/client';

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
  podeEscrever: boolean;
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
  totalCurtidas: number;
  curtidoPorMim: boolean;
};

/** `GET /api/clubes/{clubeId}/posts` — já ordenado do mais recente para o mais antigo. */
export function buscarForumDoClube(clubeId: string, signal?: AbortSignal) {
  return apiGet<ForumClube>(
    `/api/clubes/${encodeURIComponent(clubeId)}/posts`,
    { signal }
  );
}

/** `POST /api/clubes/{clubeId}/posts` — autor e data são definidos no servidor. */
export function publicarPostDoClube(
  clubeId: string,
  texto: string,
  signal?: AbortSignal
) {
  return apiRequest<PostForum>(
    `/api/clubes/${encodeURIComponent(clubeId)}/posts`,
    { method: 'POST', body: JSON.stringify({ texto }), signal }
  );
}
