import { apiGet, apiRequest } from '@/api/client';

export type Comunidade = {
  id: string;
  nome: string;
  imagemUrl: string | null;
  seguidores: number;
  sigo: boolean;
  posts: PostComunidade[];
};

export type PostComunidade = {
  id: string;
  autor: { id: string; nome: string; avatarUrl: string | null };
  publicadoEm: string;
  texto: string;
  totalCurtidas: number;
  curtidoPorMim: boolean;
  totalComentarios: number;
};

export type EstadoMembroComunidade = {
  seguidores: number;
  sigo: boolean;
};

export function buscarComunidade(comunidadeId: string, signal?: AbortSignal) {
  return apiGet<Comunidade>(
    `/api/comunidades/${encodeURIComponent(comunidadeId)}`,
    { signal }
  );
}

export function seguirComunidade(comunidadeId: string, signal?: AbortSignal) {
  return apiRequest<EstadoMembroComunidade>(
    `/api/comunidades/${encodeURIComponent(comunidadeId)}/membros/eu`,
    { method: 'POST', signal }
  );
}

export function deixarDeSeguirComunidade(
  comunidadeId: string,
  signal?: AbortSignal
) {
  return apiRequest<EstadoMembroComunidade>(
    `/api/comunidades/${encodeURIComponent(comunidadeId)}/membros/eu`,
    { method: 'DELETE', signal }
  );
}
