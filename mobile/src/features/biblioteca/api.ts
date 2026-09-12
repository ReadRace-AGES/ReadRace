import { apiGet } from '@/api/client';

export type LivroBiblioteca = {
  livroId: string;
  titulo: string;
  autor: string | null;
  capaUrl: string | null;
};

/**
 * As quatro listas da aba Meus Livros, cada uma na ordem de atividade mais
 * recente. Um favorito também aparece na lista do seu estado de leitura.
 */
export type BibliotecaResponse = {
  favoritos: LivroBiblioteca[];
  lendo: LivroBiblioteca[];
  desejo: LivroBiblioteca[];
  lidos: LivroBiblioteca[];
};

/** `GET /api/biblioteca` — a biblioteca inteira do usuário atual. */
export function buscarBiblioteca(signal?: AbortSignal) {
  return apiGet<BibliotecaResponse>('/api/biblioteca', { signal });
}
