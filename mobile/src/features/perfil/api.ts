import { apiGet } from '@/api/client';

export type Perfil = {
  id: string;
  nome: string;
  username: string;
  avatar: string | null;
  titulo: string;
  nivel: number;
  /** XP total persistido. O schema não define XP necessário por nível. */
  xpAtual: number;
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

export function buscarPerfil(usuarioId: string, signal: AbortSignal) {
  return apiGet<Perfil>(
    `/api/usuarios/${encodeURIComponent(usuarioId)}/perfil`,
    { signal }
  );
}
