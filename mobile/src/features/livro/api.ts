import { apiGet } from '@/api/client';

export type LivroDetalhe = {
  livro: {
    id: string;
    titulo: string;
    autor: string | null;
    capaUrl: string | null;
    genero: string | null;
    totalPaginas: number;
  };
  progresso: {
    paginaAtual: number;
    paginaMaximaAlcancada: number;
    percentual: number;
    concluido: boolean;
  } | null;
  posts: {
    id: string;
    autor: {
      nome: string;
      avatarUrl: string | null;
      sequenciaDias: number | null;
    };
    texto: string;
    criadoEm: string;
    curtidas: number;
  }[];
};

export function buscarDetalhe(livroId: string, signal: AbortSignal) {
  return apiGet<LivroDetalhe>(`/api/livros/${encodeURIComponent(livroId)}`, {
    signal,
  });
}
