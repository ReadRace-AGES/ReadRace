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
  usuario: {
    nome: string;
    sequenciaDias: number;
    /** Já existe registro de leitura hoje: a chama do cabeçalho acende. */
    leuHoje: boolean;
    /** Dias até a sequência acabar sem leitura (7 = leu ontem, 1 = acaba amanhã). */
    diasAtePerder: number | null;
  };
  clubes: ClubeFeed[];
  comunidades: ComunidadeFeed[];
};

/** `GET /api/feed/comunidades` — clubes e comunidades do usuário atual, em duas listas. */
export function buscarFeedComunidades(signal?: AbortSignal) {
  return apiGet<FeedComunidades>('/api/feed/comunidades', { signal });
}

/** Um card da aba Social (#197): o que alguém que eu sigo leu de um livro em um dia. */
export type AtividadeSocial = {
  tipo: 'leitura';
  usuario: {
    id: string;
    nome: string;
    avatarUrl: string | null;
    sequenciaDias: number;
  };
  livro: {
    id: string;
    titulo: string;
    autor: string | null;
    totalPaginas: number;
  };
  /** Páginas novas do dia, acima da maior página já alcançada naquele livro. */
  paginas: number;
  xp: number;
  /** Último registro do dia, com o deslocamento de São Paulo (ex.: `-03:00`). */
  em: string;
};

export type FeedSocial = { atividades: AtividadeSocial[] };

/** `GET /api/feed/social` — a leitura de quem o usuário atual segue, da mais recente à mais antiga. */
export function buscarFeedSocial(signal?: AbortSignal) {
  return apiGet<FeedSocial>('/api/feed/social', { signal });
}

export function criarClube(request: CriarClubeRequest, signal?: AbortSignal) {
  return apiRequest<ClubeCriadoResponse>('/api/clubes', {
    method: 'POST',
    body: JSON.stringify(request),
    signal,
  });
}
