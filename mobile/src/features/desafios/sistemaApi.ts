import { apiGet } from '@/api/client';

export type DesafioSistema = {
  titulo: string | null;
  descricao: string;
  meta: number;
  progresso: number;
  unidade: 'livros' | 'paginas';
  diasRestantes: number;
  xp: number;
  cumprido: boolean;
};

export type DesafiosSistemaResponse = {
  mensal: DesafioSistema;
  semanal: DesafioSistema;
};

export function consultarDesafiosSistema(signal?: AbortSignal) {
  return apiGet<DesafiosSistemaResponse>('/api/desafios/sistema', { signal });
}
