import { apiGet, apiRequest } from '@/api/client';

export type Oponente = {
  id: string;
  username: string;
  avatarUrl: string | null;
};

export type OponentesResponse = {
  oponentes: Oponente[];
};

export type CriarDesafioRequest = {
  oponenteId: string;
  tipoMeta: 'paginas';
  meta: number;
  prazoDias: number;
};

export type LivroDesafio = {
  id: string;
  titulo: string;
  autor: string | null;
  capaUrl: string | null;
};

type DesafioBase = {
  id: string;
  oponente: Oponente;
  descricao: string;
  prazoDias: number;
  diasRestantes: number;
  status: 'pendente' | 'em_andamento' | 'concluido_ganho' | 'concluido_perdido';
  progresso: {
    voce: number;
    oponente: number;
  };
};

export type DesafioResponse = DesafioBase &
  (
    | { tipoMeta: 'paginas'; meta: number; livro: null }
    | { tipoMeta: 'livro'; meta?: never; livro: LivroDesafio }
  );

export function buscarOponentes(termo: string, signal?: AbortSignal) {
  const termoLimpo = termo.trim();
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
