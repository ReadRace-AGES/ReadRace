import { apiRequest } from '@/api/client';

/** Estado da curtida do usuário atual em um post, depois de curtir ou descurtir (#101). */
export type CurtidaResponse = {
  curtidoPorMim: boolean;
  totalCurtidas: number;
};

/** `POST /api/posts/{postId}/curtida` — idempotente: curtir de novo não duplica. */
export function curtir(postId: string, signal?: AbortSignal) {
  return apiRequest<CurtidaResponse>(
    `/api/posts/${encodeURIComponent(postId)}/curtida`,
    { method: 'POST', signal }
  );
}

/** `DELETE /api/posts/{postId}/curtida` — idempotente: descurtir um post nunca curtido não falha. */
export function descurtir(postId: string, signal?: AbortSignal) {
  return apiRequest<CurtidaResponse>(
    `/api/posts/${encodeURIComponent(postId)}/curtida`,
    { method: 'DELETE', signal }
  );
}
