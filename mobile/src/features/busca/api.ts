import { apiGet } from '@/api/client';

export const TIPOS_BUSCA = ['livros', 'usuarios', 'comunidades'] as const;
export type TipoBusca = (typeof TIPOS_BUSCA)[number];

export type LivroBusca = {
  id: string;
  titulo: string;
  autor: string | null;
  capa: string | null;
  totalPaginas: number | null;
};

export type UsuarioBusca = {
  id: string;
  nome: string;
  username: string;
  avatar: string | null;
  titulo: string;
};

export type ComunidadeBusca = {
  id: string;
  nome: string;
  descricao: string | null;
  capa: string | null;
};

export type ResultadoBusca =
  | { tipo: 'livros'; itens: LivroBusca[] }
  | { tipo: 'usuarios'; itens: UsuarioBusca[] }
  | { tipo: 'comunidades'; itens: ComunidadeBusca[] };

export function buscar(termo: string, tipo: TipoBusca, signal?: AbortSignal) {
  const query = `q=${encodeURIComponent(termo)}&tipo=${tipo}`;
  return apiGet<ResultadoBusca>(`/api/busca?${query}`, { signal });
}
