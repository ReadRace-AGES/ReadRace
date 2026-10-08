import { apiGet, apiRequest } from '@/api/client';

export type CriarClubeRequest = {
  nome: string;
  descricao: string | null;
  livroId: string;
  membros: string[];
};

export type ClubeCriadoResponse = {
  id: string;
  nome: string;
  livro: { titulo: string; capaUrl: string | null };
};

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

export function criarClube(request: CriarClubeRequest, signal?: AbortSignal) {
  return apiRequest<ClubeCriadoResponse>('/api/clubes', {
    method: 'POST',
    body: JSON.stringify(request),
    signal,
  });
}
