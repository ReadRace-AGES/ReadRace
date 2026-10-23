import { apiGet, apiRequest } from '@/api/client';

/**
 * Comentário de post de comunidade, em thread (#150). Também é o corpo do `201` ao
 * comentar, com `respostas` vazio. Curtir usa a rota do post (`@/features/posts/api`).
 */
export type Comentario = {
  id: string;
  texto: string;
  publicadoEm: string;
  autor: { id: string; nome: string | null; avatarUrl: string | null };
  totalCurtidas: number;
  curtidoPorMim: boolean;
  respostas: Comentario[];
};

/** `podeComentar` é `true` para quem segue a comunidade do post. */
export type ComentariosDoPost = {
  comentarios: Comentario[];
  podeComentar: boolean;
};

/** `GET /api/posts/{postId}/comentarios` — a thread em árvore, do mais antigo para o mais novo. */
export function listarComentarios(postId: string, signal?: AbortSignal) {
  return apiGet<ComentariosDoPost>(
    `/api/posts/${encodeURIComponent(postId)}/comentarios`,
    { signal }
  );
}

/**
 * `POST /api/posts/{id}/comentarios` — com o id do post, comenta; com o id de um
 * comentário, responde a ele. 403 para quem não segue a comunidade.
 */
export function comentarPost(
  postOuComentarioId: string,
  texto: string,
  signal?: AbortSignal
) {
  return apiRequest<Comentario>(
    `/api/posts/${encodeURIComponent(postOuComentarioId)}/comentarios`,
    { method: 'POST', body: JSON.stringify({ texto }), signal }
  );
}
