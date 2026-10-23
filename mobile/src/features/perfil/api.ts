import { apiGet, apiRequest } from '@/api/client';

export type Perfil = {
  id: string;
  nome: string;
  username: string;
  email: string | null;
  avatar: string | null;
  titulo: string;
  nivel: number;
  xpAtual: number;
  xpNoNivel: number;
  xpDoNivel: number;
  seguidores: number;
  seguindo: number;
  sigo: boolean;
  meSegue: boolean;
  estatisticas: {
    livrosLidos: number;
    paginasLidas: number;
    sequenciaDias: number;
    conquistas: number;
  };
  conquistas: {
    id: string;
    nome: string;
    icone: string | null;
    descricao: string;
    desbloqueada: boolean;
    data: string | null;
  }[];
  livrosFavoritos: {
    id: string;
    titulo: string;
    autor: string | null;
    capa: string | null;
  }[];
  /** Só no perfil próprio (/api/me/perfil). */
  mascote?: { nome: string; imagem: string };
};

export type RelacaoSeguir = Pick<Perfil, 'seguidores' | 'sigo' | 'meSegue'>;

export function buscarUsuarioAtual(signal: AbortSignal) {
  return apiGet<{ id: string }>('/api/me', { signal });
}

export function alterarSeguir(
  usuarioId: string,
  seguir: boolean,
  signal: AbortSignal
) {
  return apiRequest<RelacaoSeguir>(
    `/api/usuarios/${encodeURIComponent(usuarioId)}/seguidores/eu`,
    { method: seguir ? 'POST' : 'DELETE', signal }
  );
}

export function buscarPerfil(
  usuarioId: string | undefined,
  signal: AbortSignal
) {
  const caminho =
    usuarioId === undefined
      ? '/api/me/perfil'
      : `/api/usuarios/${encodeURIComponent(usuarioId)}/perfil`;
  return apiGet<Perfil>(caminho, { signal });
}

export type AtualizacaoPerfil = {
  nome?: string;
  username?: string;
};

export function atualizarPerfil(dados: AtualizacaoPerfil) {
  return apiRequest<Perfil>('/api/me/perfil', {
    method: 'PATCH',
    body: JSON.stringify(dados),
  });
}
