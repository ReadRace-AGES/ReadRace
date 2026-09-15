import { apiGet } from '@/api/client';

export type LivroBiblioteca = {
  livroId: string;
  titulo: string;
  autor: string | null;
  capaUrl: string | null;
};

/** As quatro listas da aba Meus Livros, no nome que a API usa no path. */
export const LISTAS_BIBLIOTECA = [
  'favoritos',
  'lendo',
  'desejo',
  'lidos',
] as const;
export type ListaBiblioteca = (typeof LISTAS_BIBLIOTECA)[number];

/** Uma página de uma lista; `proximoCursor` nulo significa que a lista acabou. */
export type PaginaBiblioteca = {
  itens: LivroBiblioteca[];
  proximoCursor: string | null;
};

/**
 * Primeira página de cada lista, cada uma na ordem de atividade mais
 * recente. Um favorito também aparece na lista do seu estado de leitura.
 */
export type BibliotecaResponse = Record<ListaBiblioteca, PaginaBiblioteca>;

/** Tamanho de página que a tela pede: cinco linhas de quatro capas. */
export const TAMANHO_PAGINA = 20;

/** `GET /api/biblioteca` — a primeira página das quatro listas do usuário atual. */
export function buscarBiblioteca(signal?: AbortSignal) {
  return apiGet<BibliotecaResponse>(
    `/api/biblioteca?limite=${TAMANHO_PAGINA}`,
    { signal }
  );
}

/** `GET /api/biblioteca/{lista}?cursor=` — a página seguinte de uma lista. */
export function buscarPaginaBiblioteca(
  lista: ListaBiblioteca,
  cursor: string,
  signal?: AbortSignal
) {
  const query = `cursor=${encodeURIComponent(cursor)}&limite=${TAMANHO_PAGINA}`;
  return apiGet<PaginaBiblioteca>(`/api/biblioteca/${lista}?${query}`, {
    signal,
  });
}
