import { apiGet, apiRequest } from '@/api/client';

export type DesafioStatus =
  | 'pendente'
  | 'em_andamento'
  | 'concluido_ganho'
  | 'concluido_perdido'
  | 'concluido_empate';

export type LivroDesafio = {
  id: string;
  titulo: string;
  autor: string | null;
  capaUrl: string | null;
};

type DesafioBase = {
  id: string;
  status: DesafioStatus;
  descricao: string;
  diasRestantes: number;
  oponente: {
    id: string;
    username: string;
    avatarUrl: string | null;
  };
  progresso: {
    voce: number;
    oponente: number;
  };
};

export type Desafio = DesafioBase &
  (
    | { tipoMeta: 'paginas'; meta: number; livro: null }
    | { tipoMeta: 'livro'; meta?: never; livro: LivroDesafio }
  );

export type Oponente = {
  id: string;
  username: string;
  avatarUrl: string | null;
};

export type OponentesResponse = {
  oponentes: Oponente[];
};

type CriarDesafioBaseRequest = {
  oponenteId: string;
  prazoDias: number;
};

export type CriarDesafioRequest = CriarDesafioBaseRequest &
  (
    | { tipoMeta: 'paginas'; meta: number; livroId?: never }
    | { tipoMeta: 'livro'; livroId: string; meta?: never }
  );

export type DesafiosResponse = {
  desafios: Desafio[];
  nextCursor: string | null;
};

export function listarDesafios(signal?: AbortSignal, cursor?: string) {
  const query = cursor ? `?cursor=${encodeURIComponent(cursor)}` : '';

  return apiRequest<DesafiosResponse>(`/api/desafios${query}`, {
    method: 'GET',
    signal,
  });
}

export async function listarTodosOsDesafios(signal?: AbortSignal) {
  const primeira = await listarDesafios(signal);
  const desafios = [...primeira.desafios];
  let cursor = primeira.nextCursor;

  while (cursor) {
    const pagina = await listarDesafios(signal, cursor);
    desafios.push(...pagina.desafios);
    cursor = pagina.nextCursor;
  }

  return desafios;
}

export type DesafioResponse = DesafioBase &
  (
    | { tipoMeta: 'paginas'; meta: number; livro: null }
    | { tipoMeta: 'livro'; meta?: never; livro: LivroDesafio }
  );

export function normalizarTermoOponente(termo: string) {
  return termo.trim().replace(/^@+/, '').trim();
}

export function buscarOponentes(termo: string, signal?: AbortSignal) {
  const termoLimpo = normalizarTermoOponente(termo);
  const query = termoLimpo ? `?q=${encodeURIComponent(termoLimpo)}` : '';

  return apiGet<OponentesResponse>(`/api/desafios/oponentes${query}`, {
    signal,
  });
}

export function criarDesafio(request: CriarDesafioRequest) {
  return apiRequest<DesafioResponse>('/api/desafios', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
