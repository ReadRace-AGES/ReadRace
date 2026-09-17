import { apiGet } from '@/api/client';

export type ClubeFeed = {
  id: string;
  nome: string;
  capaUrl: string | null;
  livroAtual: { titulo: string; autor: string | null };
  totalMembros: number;
};

export type ComunidadeFeed = {
  id: string;
  nome: string;
  capaUrl: string | null;
  totalMembros: number;
};

export type FeedComunidades = {
  usuario: { nome: string; sequenciaDias: number };
  clubes: ClubeFeed[];
  comunidades: ComunidadeFeed[];
};

/** `GET /api/feed/comunidades` — clubes e comunidades do usuário atual, em duas listas. */
export function buscarFeedComunidades(signal?: AbortSignal) {
  return apiGet<FeedComunidades>('/api/feed/comunidades', { signal });
}
