import { apiRequest } from '@/api/client';

export type Desafio = {
  id: string;
  status: string;
  descricao: string;
  diasRestantes: number;
  oponente: {
    id: string;
    username: string;
    avatarUrl: string | null;
  };
  progresso: {
    voce: number;
    oponente: number;
  };
};

export type DesafiosResponse = {
  desafios: Desafio[];
  nextCursor: string | null;
};

export function listarDesafios() {
  return apiRequest<DesafiosResponse>('/api/desafios', {
    method: 'GET',
  });
}