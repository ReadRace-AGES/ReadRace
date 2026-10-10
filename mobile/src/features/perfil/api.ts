import { apiGet, apiRequest } from '@/api/client';

export type Perfil = {
  id: string;
  nome: string;
  username: string;
  avatar: string | null;
  titulo: string;
  nivel: number;
  xpAtual: number;
  xpNoNivel: number;
  xpDoNivel: number;
  seguidores: number;
  seguindo: number;
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
};

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
