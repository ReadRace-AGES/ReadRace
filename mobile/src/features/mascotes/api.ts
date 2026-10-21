import { apiGet, apiRequest } from '@/api/client';

export type Mascote = {
  id: string;
  nome: string;
  imagem: string;
  nivelMinimo: number;
  desbloqueado: boolean;
  escolhido: boolean;
};

export function buscarMascotes(signal: AbortSignal) {
  return apiGet<{ mascotes: Mascote[] }>('/api/me/mascotes', { signal }).then(
    (resposta) => resposta.mascotes
  );
}

export function escolherMascote(mascoteId: string) {
  return apiRequest<Pick<Mascote, 'id' | 'nome' | 'imagem'>>(
    '/api/me/mascote',
    { method: 'PUT', body: JSON.stringify({ mascoteId }) }
  );
}
