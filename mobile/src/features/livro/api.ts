import { apiGet, apiRequest } from '@/api/client';

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
    statusLeitura?: 'lendo' | 'lido' | 'desejo';
    favorito?: boolean;
  } | null;
  posts: PostLivro[];
};

export type PostLivro = {
  id: string;
  autor: {
    nome: string;
    avatarUrl: string | null;
    sequenciaDias: number | null;
  };
  texto: string;
  criadoEm: string;
  curtidas: number;
  curtidoPorMim: boolean;
  totalComentarios: number;
  meu: boolean;
};

export function buscarDetalhe(livroId: string, signal: AbortSignal) {
  return apiGet<LivroDetalhe>(`/api/livros/${encodeURIComponent(livroId)}`, {
    signal,
  });
}

export function publicarPostDoLivro(
  livroId: string,
  texto: string,
  signal?: AbortSignal
) {
  return apiRequest<PostLivro>(
    `/api/livros/${encodeURIComponent(livroId)}/posts`,
    { method: 'POST', body: JSON.stringify({ texto }), signal }
  );
}

export function apagarPostDoLivro(livroId: string, postId: string) {
  return apiRequest<void>(
    `/api/livros/${encodeURIComponent(livroId)}/posts/${encodeURIComponent(postId)}`,
    { method: 'DELETE' }
  );
}
